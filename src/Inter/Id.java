package main.src.Inter;

import main.src.Lexer.*;
import main.src.Symbols.*;

public class Id extends Expr {
	public int offset;

	public Id( Word id, Type p, int b) {
		super(id, p);
		offset = b;
	}
}
