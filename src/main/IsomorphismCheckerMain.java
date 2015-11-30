package main;
import java.io.IOException;
import java.text.ParseException;

import algorithms.AutomatonGVLoader;
import algorithms.AutomatonIsomorphismChecker;
import structures.Automaton;


public class IsomorphismCheckerMain {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Tool for EFSM isomorphism checking");
			System.out.println("Author: Vladimir Ulyantsev, ulyantsev@rain.ifmo.ru\n");
			System.out.println("Usage: java -jar checker.jar <first.gv> <second.gv>");
			return;
		}
		
		Automaton first, second;
		try {
			first = AutomatonGVLoader.load(args[0]);
		} catch (IOException e) {
			System.err.println("Can't open file " + args[0]);
			e.printStackTrace();
			return;
		} catch (ParseException e) {
			System.err.println("Can't read EFSM from file " + args[0]);
			e.printStackTrace();
			return;
		}
		
		try {
			second = AutomatonGVLoader.load(args[1]);
		} catch (IOException e) {
			System.err.println("Can't open file " + args[1]);
			e.printStackTrace();
			return;
		} catch (ParseException e) {
			System.err.println("Can't read EFSM from file " + args[1]);
			e.printStackTrace();
			return;
		}
		
		boolean ans = AutomatonIsomorphismChecker.isIsomorphic(first, second);		
		System.out.println(ans ? "ISOMORPHIC" : "NOT ISOMORPHIC");
	}

}
