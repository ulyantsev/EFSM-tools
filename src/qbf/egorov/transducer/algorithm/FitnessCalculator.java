package qbf.egorov.transducer.algorithm;

import java.util.ArrayList;
import java.util.List;

import qbf.egorov.transducer.scenario.Path;
import qbf.egorov.transducer.scenario.TestGroup;
import qbf.egorov.transducer.verifier.IVerifierFactory;

public class FitnessCalculator {
	
	private final double TESTS_COST = 1;
	private final double FORMULAS_COST = TESTS_COST;
	
	private final ArrayList<Path> tests;
    private final List<TestGroup> groups;
	
    private final IVerifierFactory verifier;
    private boolean hasFormulas;
	
	public FitnessCalculator(IVerifierFactory verifier, List<TestGroup> groups, ArrayList<Path> tests) {
        this.groups = groups;
		this.tests = tests;
        this.verifier = verifier;
        
        for (TestGroup g : groups) {
        	if (!g.getFormulas().isEmpty()) {
        		hasFormulas = true;
        		break;
        	}
        }
	}
	
	public ArrayList<Path> getTests() {
		return tests;
	}

    public double evaluateDesiredFitness(List<TestGroup> groups) {
        /*double res = 0;
        for (TestGroup g : groups) {
            if (!g.getFormulas().isEmpty()) {
                res += FORMULAS_COST;
            }
            if (!g.getTests().isEmpty()) {
                res += TESTS_COST;
            }
        }
        return res;*/
        return groups.size() * 2;
    }
	
	public double calcFitnessForTest(FST fst, Path test) {
		String[] output = fst.transform(test.getInput());
		String[] answer = test.getOutput();
		double t;
		if (output == null) {
			t = 1;
		} else {
			t = editDistance(output, answer) / Math.max(output.length, answer.length);
		}
		return 1 - t;
	}

	public double calcFitness(FST fst) {
        int transitionCount = fst.getUsedTransitionsCount();
        if (transitionCount == 0) {
            return 0.0;
        }

        fst.unmarkAllTransitions();
		fst.doLabelling(this.tests);
		
		int[] verRes = null;
		if (hasFormulas) {
			verifier.configureStateMachine(fst);
			verRes = verifier.verify();
		}

        double res = 0; //fitness
        int i = 0;      //group number
        for (TestGroup group : groups) {
            double pSum = 0;   //positive tests sum
            int nSum = 0;      //negative tests sum
            int cntOk = 0;

            for (Path p : group.getTests()) {
                String[] output = fst.transform(p.getInput());
                String[] answer = p.getOutput();
                double t;
                if (output == null) {
                    t = 1;
                } else {
                    t = editDistance(output, answer) / Math.max(output.length, answer.length);
                }
                pSum += 1 - t;
                if (same(output, answer)) {
                    cntOk++;
                }
            }

            for (Path p : group.getNegativeTests()) {
                if (!fst.validateNegativeTest(p.getInput())) {
                    nSum++;
                }
            }

            int testsSize = group.getTests().size();
            int negativeTestsSize = group.getNegativeTests().size();
            int formulasSize = group.getFormulas().size();

            double testsFF = TESTS_COST;
            if (testsSize > 0) {
                testsFF = (cntOk == testsSize) ? TESTS_COST : TESTS_COST * (pSum / testsSize);
            }
            double negativeTestsFF = (negativeTestsSize != 0) ? (nSum * 1.0) / negativeTestsSize : 0;
            double ltlFF = FORMULAS_COST;
            if (formulasSize > 0) {
                ltlFF = FORMULAS_COST * verRes[i] / formulasSize / transitionCount;
                if ((ltlFF > FORMULAS_COST) || (ltlFF < 0)) {
                    throw new RuntimeException(String.valueOf(ltlFF));
                }
            }
//            res += testsFF + testsFF * ltlFF - negativeTestsFF * testsFF;
            res += testsFF + ltlFF - negativeTestsFF;
            i++;
        }
		return res + 0.0001 * (100 - fst.getTransitionsCount());
	}
	
	public double correctFitness(FST cachedInstance, FST trueInstance) {
		 double negativeTerm = 0.0001 * (100 - cachedInstance.getTransitionsCount());
         double positiveTerm = 0.0001 * (100 - trueInstance.getTransitionsCount());
         return positiveTerm - negativeTerm;

	}
	
	public List<String> getFailedTests(FST fst) {
		List<String> result = new ArrayList<String>();
		for (TestGroup group : groups) {
			double pSum = 0;   //positive tests sum
			int nSum = 0;      //negative tests sum
			int cntOk = 0;

			for (Path p : group.getTests()) {
				String[] output = fst.transform(p.getInput());
				String[] answer = p.getOutput();
				double t;
				if (output == null) {
					t = 1;
				} else {
					t = editDistance(output, answer) / Math.max(output.length, answer.length);
				}
				pSum += 1 - t;
				if (!same(output, answer)) {
					StringBuilder ans = new StringBuilder();
					StringBuilder out = new StringBuilder();
					StringBuilder in = new StringBuilder();
					
					for (String s : p.getInput()) {
						in.append(s);
						in.append(",");
					}
 					
					if (output != null) {
						for (String s : output) {
							out.append(s);
							out.append(",");
						}
					}
					for (String s : answer) {
						ans.append(s);
						out.append(",");
					}
					result.add(in + " ---- " + out + " ---- " + ans);
				}
			}
		}

		 return result;
	}
	
	private boolean same(String[] output, String[] answer) {
		if (output == null || answer == null) {
			return false;
		}
		if (output.length != answer.length) {
			return false;
		}
		for (int i = 0; i < answer.length; i++) {
			if (!answer[i].equals(output[i])) {
				return false;
			}
		}
		return true;
	}

	private double editDistance(String[] a, String[] b) {
		int n = a.length;
		int m = b.length;
		double[][] d = new double[n + 1][m + 1];
		
		for (int i = 0; i <= n; i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = j;
		}
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				int cost;
				if (a[i].equals(b[j])) {
					cost = 0;
				} else {
					cost = 1;
				}
				d[i + 1][j + 1] = Math.min(Math.min(
									d[i][j + 1] + 1, 
									d[i + 1][j] + 1), 
									d[i][j] + cost);
			}
		}
		
		return d[n][m];
	}

	private double hammingDistance(String[] a, String[] b) {
		int n = a.length;
		int m = b.length;
//		if (n != m) {
//			throw new RuntimeException();
//		}
		int res = 0;
		for (int i = 0; i < Math.min(n, m); i++) {
			if (!a[i].equals(b[i])) {
				res++;
			}
		}
		res += Math.max(n, m) - Math.min(n, m);
		return res;
	}
	
	
}
