package qbf.egorov.transducer;

public class Transition {
	private final String input;
	private String[] output;
	private final int newState;
		
	public Transition(String input, int newState, String[] output) {
		this.input = input;
		this.newState = newState;
		this.output = output;
	}

    public String input() {
        return input;
    }
	
	public String[] output() {
		return output;
	}

	public int newState() {
		return newState;
	}
	
	@Override
	public String toString() {
		return input + " / "
				+ (output == null ? "null" : String.join(" ", output))
				+ ", " + newState; 
	}
}
