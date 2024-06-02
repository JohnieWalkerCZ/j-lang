package main.src.Inter.Stmts;

import main.src.Inter.Expr;
import main.src.Inter.Stmt;

public class Print extends Stmt {
    Expr expr;

    public Print(Expr x) {
        expr = x;
    }

    public void gen(int b, int a) {
        emit("print: " + expr.toString());
    }
}