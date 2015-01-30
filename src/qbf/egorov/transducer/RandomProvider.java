package qbf.egorov.transducer;

import java.util.Random;

public class RandomProvider {

	private static Random rnd = new Random();
	
	public static Random getInstance() {
		return rnd;
	}

}
