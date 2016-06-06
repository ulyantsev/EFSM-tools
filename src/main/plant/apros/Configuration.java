package main.plant.apros;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import structures.plant.NondetMooreAutomaton;

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
	
	public List<String> actionDescriptions() {
		final List<String> result = new ArrayList<>();
		for (Parameter p : outputParameters) {
			result.addAll(p.descriptions());
		}
		return result;
	}

    public Map<String, String> extendedActionDescriptions() {
        final Map<String, String> descriptions = new LinkedHashMap<>();
        final List<String> actions = actions();
        final List<String> actionDescriptions = actionDescriptions();
        for (int i = 0; i < actions.size(); i++) {
            descriptions.put(actions.get(i), actionDescriptions.get(i));
        }
        return descriptions;
    }
	
	public List<String> actions() {
		final List<String> result = new ArrayList<>();
		for (Parameter p : outputParameters) {
			result.addAll(p.traceNames());
		}
		return result;
	}

    public List<Pair<String, Parameter>> actionThresholds() {
        final List<Pair<String, Parameter>> actionThresholds = new ArrayList<>();
        for (Parameter p : outputParameters) {
            actionThresholds.add(Pair.of(p.traceName(), p));
        }
        return actionThresholds;
    }

    public List<Pair<String, Parameter>> eventThresholds() {
        final List<Pair<String, Parameter>> eventThresholds = new ArrayList<>();
        for (Parameter p : inputParameters) {
            eventThresholds.add(Pair.of(p.traceName(), p));
        }
        return eventThresholds;
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