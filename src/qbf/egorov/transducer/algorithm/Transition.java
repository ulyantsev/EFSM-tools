package qbf.egorov.transducer.algorithm;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import qbf.egorov.transducer.RandomProvider;

public class Transition {
	
	final String input;
	private int outputSize;
	private String[] output;
	
	private final int newState;
	
	private final HashMap<String, Integer> map;

	boolean used;
    boolean usedByNegativeTest;
    boolean usedByVerifier;
    boolean verified;
	
	public Transition(String input, int outputSize, int newState) {
		this.input = input;
		this.outputSize = outputSize;
		this.newState = newState;
		map = new HashMap<String, Integer>();
	}
	
	public boolean accepts(String s) {
		return input.equals(s) || input.equals("*");
	}

    public String getInput() {
        return input;
    }
	
    public void setOutput(String[] output) {
    	this.output = output;
    }
    
	public String[] getOutput() {
		return output;
	}

	public int getNewState() {
		return newState;
	}

	public Transition mutate(String[] setOfInputs, String[] setOfOutputs, int stateNumber) {
		int type = RandomProvider.getInstance().nextInt(3);
		Transition result = null;
		if (type == 0) {
			result = new Transition(input, outputSize, RandomProvider.getInstance().nextInt(stateNumber));
		} else if (type == 1) {
			result = new Transition(setOfInputs[RandomProvider.getInstance().nextInt(setOfInputs.length)], outputSize, newState);
		} else {
            // Change output size
			result = new Transition(input, mutateOutputSize(outputSize, setOfOutputs.length), newState);
		}
		result.setOutput(output);
		return result;
	}

	public Transition copy(String[] setOfInputs, String[] setOfOutputs, int stateNumber) {
		Transition result = new Transition(input, outputSize, newState);
		result.setOutput(output);
		result.used = used;
		return result;
	}
	
	@Override
	public String toString() {
		String s = "";
		if (output == null) {
			s += " null"; 
		} else {
			for (String s1 : output) {
				s += " " + s1;
			}
		}
		s = s.trim();
		return input + " / " + s + ", " + newState; 
	}
	
	private int mutateOutputSize(int outputSize, int outputCnt) {
		if (RandomProvider.getInstance().nextBoolean()) {
			return Math.min(outputSize + 1, outputCnt);
		} else {
			return Math.max(0, outputSize - 1);
		}
	}
	
	private String[] mutateArray(String[] array, String[] setOfOutputs) {
		ArrayList<String> list = new ArrayList<String>();
		for (String s : array) {
			list.add(s);
		}
		if (RandomProvider.getInstance().nextBoolean() && list.size() > 0) {
			list.remove(RandomProvider.getInstance().nextInt(list.size()));
		} else {
			list.add(RandomProvider.getInstance().nextInt(list.size() + 1), setOfOutputs[RandomProvider.getInstance().nextInt(setOfOutputs.length)]);
		}
		return list.toArray(new String[list.size()]);
	}

	public int getOutputSize() {
		return outputSize;
	}

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

	public void addOutputSequence(String s) {
        if (StringUtils.isEmpty(s)) {
            s = "null";
        }
		Integer i = map.get(s);
		if (i == null) {
			i = 0;
		}
		map.put(s, i + 1);
	}
	
	public void labelByMostFrequent() {
		int max = -1;
		String best = null;
		for (String s : map.keySet()) {
			int cur = map.get(s);
			if (cur > max) {
				max = cur;
				best = s;
			}
		}
		if ((best == null) || ("null".equals(best))) {
			output = new String[0];
		} else {
			output = best.split(",");
		}
	}

	public void beginLabeling() {
		if (map.size() > 0) {
			throw new RuntimeException();
		}
		map.clear();
	}

	public void markUnused() {
		used = false;
	}

	public void markUsed() {
		used = true;
	}

    public boolean isUsedByNegativeTest() {
        return usedByNegativeTest;
    }

    public void setUsedByNegativeTest(boolean usedByNegativeTest) {
        this.usedByNegativeTest = usedByNegativeTest;
    }

    /**
     * is this transition in the counterexample
     * @return true if transition in the counterexample
     */
    public boolean isUsedByVerifier() {
        return usedByVerifier;
    }

    public void setUsedByVerifier(boolean usedByVerifier) {
        this.usedByVerifier = usedByVerifier;
    }

    /**
     * Is this transition satisfy the LTL statement
     * @return true, if trastition satisfy
     */
    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
