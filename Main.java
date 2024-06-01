package main;

import java.io.*;
import main.src.Lexer.*;
import main.src.Parser.*;

public class Main {
	public static void main(String[] args) throws IOException {
		Lexer lexer = new Lexer();
		Parser parser = new Parser(lexer);
		parser.program();
		System.out.write('\n');
	}
}
