package main.src.Inter;

public class Stmt extends Node {
	public Stmt() {  }

	public static Stmt Null = new Stmt();

	public void gen(int b, int a) {} //  called with labels begin & after
	public int after = 0;
	public static Stmt Enclosing = Stmt.Null; // Used for 'break'
}
