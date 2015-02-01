package qbf.egorov.transducer;

public class Transition {
	final String input;
	private int outputSize;
	private String[] output;
	
	private final int newState;

	boolean used;
    boolean usedByNegativeTest;
    boolean usedByVerifier;
    boolean verified;
	
	public Transition(String input, int outputSize, int newState) {
		this.input = input;
		this.outputSize = outputSize;
		this.newState = newState;
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
	
	public int getOutputSize() {
		return outputSize;
	}

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

    public void setUsedByVerifier(boolean usedByVerifier) {
        this.usedByVerifier = usedByVerifier;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
