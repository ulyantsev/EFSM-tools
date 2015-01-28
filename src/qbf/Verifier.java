package qbf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

import qbf.ltl.LtlNode;

/**
 * (c) Igor Buzhinsky
 */

public class Verifier {
	public static boolean verify(String resultFilePath, String ltlFilePath, int size, List<LtlNode> formulae, Logger logger) throws IOException {
		final String java7 = "/usr/lib/jvm/jdk7/bin/java";
		final String verifierStr = java7 + " -jar verifier.jar ../" + resultFilePath +  " " + size + " " + "../" + ltlFilePath;
		final Process verifier = Runtime.getRuntime().exec(verifierStr, new String[0], new File("./qbf"));
		int verified;
		try (BufferedReader input = new BufferedReader(new InputStreamReader(verifier.getInputStream()))) {
			verified = (int) input.lines().count();
		}
		if (verified == formulae.size()) {
			logger.info("VERIFIED");
			return true;
		} else {
			logger.severe("NOT VERIFIED");
			return false;
		}
	}
}
