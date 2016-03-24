package main.plant.apros;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Configuration {
	final double intervalSec;
	final List<Parameter> outputParameters;
	final List<Parameter> inputParameters;
	final List<String> colorRules = new ArrayList<>();

	public List<Parameter> allParameters() {
		final List<Parameter> params = new ArrayList<>(outputParameters);
		params.addAll(inputParameters);
		return params;
	}
	
	public Configuration(double intervalSec,
			List<Parameter> outputParameters,
			List<Parameter> inputParameters) {
		this.intervalSec = intervalSec;
		this.outputParameters = outputParameters;
		this.inputParameters = inputParameters;
	}

	public void addColorRule(Parameter param, int index, String color) {
		colorRules.add(param.traceName(index) + "->" + color);
	}
	
	@Override
	public String toString() {
		return "out:\n  " +  
				String.join("\n  ", outputParameters.stream()
						.map(p -> p.toString()).collect(Collectors.toList()))
				+ "\nin:\n  " + 
				String.join("\n  ", inputParameters.stream()
					.map(p -> p.toString()).collect(Collectors.toList()));
	}
}