package main.src.Inter.Stmts;

import main.src.Symbols.*;
import main.src.Inter.*;

public class If extends Stmt {
	Expr expr;
	Stmt stmt;

	public If ( Expr x, Stmt s ) {
		expr = x;
		stmt = s;

		if ( expr.type != Type.Bool ) expr.error("boolean requried in if statement");
	}

	public void gen(int b, int a) {
		int label = newlabel();
		expr.jumping(0, a);
		emitlabel(label);
		stmt.gen(label, a);
	}
}
