package qbf.reduction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class BooleanFormula {
	public abstract String toLimbooleString();
	
	public static class DimacsConversionInfo {
		private final StringBuilder dimacsBuilder = new StringBuilder();
		private final Map<Integer, Integer> limbooleNumberToDimacs = new HashMap<>();
		private final Map<Integer, Integer> dimacsNumberToLimboole = new HashMap<>();
		private String title;
		private Integer varCount;
		
		void acceptLine(String line) {
			if (line.startsWith("c")) {
				String[] tokens = line.split(" ");
				assert tokens.length == 3;
				int first = Integer.parseInt(tokens[1]);
				int second = Integer.parseInt(tokens[2]);
				limbooleNumberToDimacs.put(second, first);
				dimacsNumberToLimboole.put(first, second);
			} else if (line.startsWith("p")) {
				title = line;
				varCount = Integer.parseInt(line.split(" ")[2]);
			} else {
				dimacsBuilder.append(line + "\n");
			}
		}
		
		public String title() {
			assert title != null;
			return title;
		}
		
		public String output() {
			return dimacsBuilder.toString();
		}
		
		public Integer toDimacsNumber(int num) {
			return limbooleNumberToDimacs.get(num);
		}
		
		public int varCount() {
			return varCount;
		}
		
		/*
		 * Can return null as there can be more vars in DIMACS representation.
		 */
		public Integer toLimbooleNumber(int num) {
			return dimacsNumberToLimboole.get(num);
		}
	}
	
	private static final boolean USE_COPROCESSOR = false;
	
	public DimacsConversionInfo toDimacs(Logger logger) throws IOException {
		final String beforeLimbooleFilename = "_tmp.limboole";
		DimacsConversionInfo info = new DimacsConversionInfo();

		if (!USE_COPROCESSOR) {
			PrintWriter pw = new PrintWriter(beforeLimbooleFilename);
			pw.print(toLimbooleString());
			pw.close();
	
			String limbooleStr = "limboole -d -s " + beforeLimbooleFilename;
			logger.info(limbooleStr);
			Process p = Runtime.getRuntime().exec(limbooleStr);
			try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = input.readLine()) != null) {
					info.acceptLine(line);
				}
			}
			
			return info;
		} else {
			final String afterLimbooleFilename = "_tmp.after.limboole.dimacs";
			String line;
			
			try (PrintWriter pw = new PrintWriter(beforeLimbooleFilename)) {
				pw.print(toLimbooleString());
			}
	
			// transforming formula to DIMACS
			String limbooleStr = "limboole -d -s -o " + afterLimbooleFilename + " " + beforeLimbooleFilename;
			System.out.println(limbooleStr);
			Process limboole = Runtime.getRuntime().exec(limbooleStr);
			try {
				limboole.waitFor();
			} catch (InterruptedException e) {
				throw new AssertionError();
			}
			
			// obtaining variable name mapping
			try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(afterLimbooleFilename)))) {
				while ((line = input.readLine()) != null) {
					if (line.startsWith("c")) {
						info.acceptLine(line);
					}
				}
			}
			
			// writing variables we cannot exclude from DIMACS CNF
			try (PrintWriter pw = new PrintWriter("white.var")) {
				for (int var : info.dimacsNumberToLimboole.keySet()) {
					pw.println(var);
				}
			}
			
			// simplifying CNF, not excluding important variables
			Process coprocessor = Runtime.getRuntime().exec("coprocessor " + afterLimbooleFilename + " -CP_mapFile=map.map -CP_print=1 -CP_whiteFile=white.var -CP_unlimited=1");
			
			try (BufferedReader input = new BufferedReader(new InputStreamReader(coprocessor.getInputStream()))) {
				while ((line = input.readLine()) != null) {
					info.acceptLine(line);
				}
			}
	
			return info;
		}
	}
	
	public BooleanFormula not() {
		return new NotOperation(this);
	}
	
	public BooleanFormula and(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.AND);
	}
	
	public BooleanFormula or(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.OR);
	}
	
	public BooleanFormula implies(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.IMPLIES);
	}
	
	public BooleanFormula equivalent(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.EQ);
	}
}
