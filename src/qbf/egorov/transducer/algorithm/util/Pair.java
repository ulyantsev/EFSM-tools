package qbf.egorov.transducer.algorithm.util;

import qbf.egorov.transducer.scenario.Path;

import java.util.Random;

public class Pair implements Comparable<Pair> {
	private static final double EPS = 1e-9;
	private static final Random RANDOM = new Random();
	private final double fitness;
	private final Path test;
	
	public Pair(Path test, double fitness) {
		this.test = test;
		this.fitness = fitness;
	}
	
	public int compareTo(Pair that) {
		if (Math.abs(fitness - that.fitness) < EPS && test == that.test) {
			return 0;
//			if (test == that.test) {
//				return 0;
//			} else {
				
//				if (RANDOM.nextBoolean()) {
//					return 1;
//				} else {
//					return -1;
//				}
//			}
		} else {
			return -Double.compare(fitness, that.fitness);
		}
	}

	public Path getTest() {
		return test;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair)) {
			return false;
		}
		
		Pair other = (Pair)obj;
		return test == other.test && Math.abs(fitness - other.fitness) < EPS;
 	}
	
	
	
}
