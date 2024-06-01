package main.src.Inter.Stmts;

import main.src.Inter.Stmt;

public class Break extends Stmt {
    Stmt stmt;

    public Break() {
        if (Stmt.Enclosing == Stmt.Null) error("unenclosed break");
        stmt = Stmt.Enclosing;
    }

    public void gen(int b, int a) {
        emit("goto L" + stmt.after);
    }
}
