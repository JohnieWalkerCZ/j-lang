package main.src.Parser;

import java.io.*;

import main.src.Inter.Logicals.*;
import main.src.Keywords;
import main.src.Lexer.*;
import main.src.Symbols.*;
import main.src.Inter.*;
import main.src.Inter.Stmts.*;

public class Parser {
    private final Lexer lex; // Lexer for this parser
    private Token look; // lookahead token
    Env top = null; // current or top symbol table
    int used = 0; // storage used for declarations

    public Parser(Lexer l) throws IOException {
        lex = l;
        move();
    }

    void move() throws IOException {
        look = lex.scan();
    }

    void error(String s) {
        throw new Error("near line " + lex.line + ": " + s);
    }

    void match(int t) throws IOException {
        if (look.tag == t) move();
        else error("syntax error");
    }

    public void program() throws IOException { // program -> block
        Stmt s = block();
        int begin = s.newlabel();
        int after = s.newlabel();

        s.emitlabel(begin);
        s.gen(begin, after);
        s.emitlabel(after); // Generates intermediate code
    }

    Stmt block() throws IOException { // block -> { decls, stmts }
        match(Keywords.BLOCK_START);
        Env savedEnv = top;
        top = new Env(top);
        decls();
        Stmt s = stmts();
        match(Keywords.BLOCK_END);
        top = savedEnv;
        return s;
    }

    void decls() throws IOException {
        while (look.tag == Tag.BASIC) { // D -> type ID ;
            Type p = type();
            Token tok = look;
            match(Tag.ID);
            match(Keywords.END_STMT_CHAR);
            Id id = new Id((Word) tok, p, used);
            top.put(tok, id);
            used = used + p.width;

        }
    }

    Type type() throws IOException {
        Type p = (Type) look;
        match(Tag.BASIC);
        if (look.tag != Keywords.ARRAY_START) return p; // T -> basic
        else return dims(p);          // return array type
    }

    Type dims(Type p) throws IOException {
        match(Keywords.ARRAY_START);
        Token tok = look;
        match(Tag.NUM);
        match(Keywords.ARRAY_END);
        if (look.tag == Keywords.ARRAY_START) p = dims(p);
        return new Array(((Num) tok).value, p);
    }

    Stmt stmts() throws IOException {
        if (look.tag == Keywords.BLOCK_END) return Stmt.Null;
        else return new Seq(stmt(), stmts());
    }

    Stmt stmt() throws IOException {
        Expr x;
        Stmt s, s1, s2;
        Stmt savedStmt;

        switch (look.tag) {
            case Keywords.END_STMT_CHAR:
                move();
                return Stmt.Null;
            case Tag.IF:
                match(Tag.IF); match(Keywords.STMT_OPENING); x = bool(); match(Keywords.STMT_CLOSING);
                s1 = stmt();
                if (look.tag != Tag.ELSE) return new If(x, s1);
                match(Tag.ELSE);
                s2 = stmt();
                return new Else(x, s1, s2);
            case Tag.WHILE:
                While whilenode = new While();
                savedStmt = Stmt.Enclosing;
                Stmt.Enclosing = whilenode;
                match(Tag.WHILE);
                match(Keywords.STMT_OPENING);
                x = bool();
                match(Keywords.STMT_CLOSING);
                s1 = stmt();
                whilenode.init(x, s1);
                Stmt.Enclosing = savedStmt; // reset Stmt.Enclosing
                return whilenode;
            case Tag.DO:
                Do donode = new Do();
                savedStmt = Stmt.Enclosing;
                Stmt.Enclosing = donode;
                match(Tag.DO);
                s1 = stmt();
                match(Tag.WHILE);
                match(Keywords.STMT_OPENING);
                x = bool();
                match(Keywords.STMT_CLOSING);
                match(Keywords.END_STMT_CHAR);
                donode.init(s1, x);
                Stmt.Enclosing = savedStmt; // reset Stmt.Enclosing
                return donode;
            case Tag.BREAK:
                match(Tag.BREAK);
                match(Keywords.END_STMT_CHAR);
                return new Break();
            case Keywords.BLOCK_START:
                return block();
            case Tag.PRINT:
                match(Tag.PRINT); match(Keywords.STMT_OPENING); x = factor(); match(Keywords.STMT_CLOSING);
                return new Print(x);
            default:
                return assign();
        }
    }

    Stmt assign() throws IOException {
        Stmt stmt;
        Token t = look;
        match(Tag.ID);
        Id id = top.get(t);
        if (id == null) error(t.toString() + " undeclared");
        if (look.tag == '=') {  // S -> id = E ;
            move();
            stmt = new Set(id, bool());
        } else {                // S -> L = E ;
            Access x = offset(id);
            match('=');
            stmt = new SetElem(x, bool());
        }
        match(Keywords.END_STMT_CHAR);
        return stmt;
    }

    Expr bool() throws IOException {
        Expr x = equality();
        while (look.tag == Tag.AND) {
            Token tok = look;
            move();
            x = new And(tok, x, equality());
        }
        return x;
    }

    Expr equality() throws IOException {
        Expr x = rel();
        while (look.tag == Tag.EQ || look.tag == Tag.NE) {
            Token tok = look;
            move();
            x = new Rel(tok, x, rel());
        }
        return x;
    }

    Expr rel() throws IOException {
        Expr x = expr();
        switch (look.tag) {
            case '<':
            case Tag.LE:
            case Tag.GE:
            case '>':
                Token tok = look;
                move();
                return new Rel(tok, x, expr());
            default:
                return x;
        }
    }

    Expr expr() throws IOException {
        Expr x = term();
        while (look.tag == '+' || look.tag == '-') {
            Token tok = look;
            move();
            x = new Arith(tok, x, term());
        }
        return x;
    }

    Expr term() throws IOException {
        Expr x = unary();
        while (look.tag == '*' || look.tag == '/') {
            Token tok = look;
            move();
            x = new Arith(tok, x, unary());
        }
        return x;
    }

    Expr unary() throws IOException {
        if (look.tag == '-') {
            move();
            return new Unary(Word.minus, unary());
        } else if (look.tag == '!') {
            Token tok = look;
            move();
            return new Not(tok, unary());
        } else {
            return factor();
        }

    }

    Expr factor() throws IOException {
        Expr x = null;
        switch (look.tag) {
            case Keywords.STMT_OPENING:
                move();
                x = bool();
                match(Keywords.STMT_CLOSING);
                return x;
            case Tag.NUM:
                x = new Constant(look, Type.Int);
                move();
                return x;
            case Tag.REAL:
                x = new Constant(look, Type.Float);
                move();
                return x;
            case Tag.TRUE:
                x = Constant.True;
                move();
                return x;
            case Tag.FALSE:
                x = Constant.False;
                move();
                return x;
            default:
                error("syntax error");
                return x;
            case Tag.ID:
                String s = look.toString();
                Id id = top.get(look);
                if (id == null) error(look.toString() + " undeclared");
                move();
                if (look.tag != Keywords.ARRAY_START) return id;
                else return offset(id);
        }
    }

    Access offset(Id a) throws IOException { // I -> [E] | [E] I
        Expr i;
        Expr w;
        Expr t1, t2;
        Expr loc;
        Type type = a.type;
        match(Keywords.ARRAY_START);
        i = bool();
        match(Keywords.ARRAY_END);
        type = ((Array) type).of;
        w = new Constant(type.width);
        t1 = new Arith(new Token('*'), i, w);
        loc = t1;
        while (look.tag == Keywords.ARRAY_START) { // multi-dimensional I -> [E] I
            match(Keywords.ARRAY_START);
            i = bool();
            match(Keywords.ARRAY_END);
            type = ((Array) type).of;
            w = new Constant(type.width);
            t1 = new Arith(new Token('*'), i, w);
            t2 = new Arith(new Token('/'), loc, t1);
            loc = t2;
        }
        return new Access(a, loc, type);
    }

}
