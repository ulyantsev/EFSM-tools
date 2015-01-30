package ru.ifmo.ctddev.genetic.transducer.scenario;

import java.util.ArrayList;

public class Path implements Cloneable {
	private final ArrayList<String> input;
	private final ArrayList<String> fixedOutput;
    private final ArrayList<String> output;
	
	public Path() {
		input = new ArrayList<String>();
		fixedOutput = new ArrayList<String>();
        output = new ArrayList<String>();
	}
	
	public Path appendInput(String s) {
		Path res = this;
		res.input.add(s);
		return res;
	}
	
	public Path appendOutput(String s) {
		Path res = this;
        res.output.add(s);
		return res;
	}
    
    public Path appendFixedOutput(String s) {
        Path res = this;
        res.fixedOutput.add(s);

        if (s != null) {
            for (String a : s.split(",")) {
                res.output.add(a.trim());
            }
        }
        return res;
    }
	
	public Path appendEdge(Edge e) {
		Path res = this.copy();
		for (String s : e.actions) {
			res.fixedOutput.add(s);
		}
		return res;
	}
	
	public int size() {
		return input.size() + output.size();
	}
	
	public String[] getInput() {
		return input.toArray(new String[input.size()]);
	}
	
	public String[] getFixedOutput() {
		return fixedOutput.toArray(new String[fixedOutput.size()]);
	}
    
    public String[] getOutput() {
        return output.toArray(new String[output.size()]);
    }
	
	public Path copy() {
		Path res = new Path();
		for (String s : input) {
			res.input.add(s);
		}
		for (String s : fixedOutput) {
            res.fixedOutput.add(s);
		}
        for (String s : output) {
            res.output.add(s);
        }
		return res;
	}
	
}