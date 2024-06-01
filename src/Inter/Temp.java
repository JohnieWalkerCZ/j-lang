package main.src.Inter;

import main.src.Lexer.*;
import main.src.Symbols.*;

public class Temp extends Expr {
	static int count = 0;
	int number = 0;

	public Temp( Type p ) {
		super(Word.temp, p);
		number = ++count;
	}

	public String toString() {
		return "t" + number;
	}
}
