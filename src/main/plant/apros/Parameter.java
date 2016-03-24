package main.plant.apros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class Parameter {
	final List<Double> cutoffs;
	private String traceName;
	private final String aprosName;
	private double min = Double.POSITIVE_INFINITY;
	private double max = Double.NEGATIVE_INFINITY;

	public String aprosName() {
		return aprosName;
	}
	
	public static boolean unify(Parameter p, Parameter q) {
		if (p.aprosName.equals(q.aprosName)) {
			final Set<Double> allCutoffs = new TreeSet<>(p.cutoffs);
			allCutoffs.addAll(q.cutoffs);
			p.cutoffs.clear();
			p.cutoffs.addAll(allCutoffs);
			q.cutoffs.clear();
			q.cutoffs.addAll(allCutoffs);
			q.traceName = p.traceName;
			return true;
		}
		return false;
	}
	
	public Parameter(String aprosName, String name, Double... cutoffs) {
		this.traceName = name;
		this.aprosName = aprosName;
		this.cutoffs = new ArrayList<>(Arrays.asList(cutoffs));
		this.cutoffs.add(Double.POSITIVE_INFINITY);
	}

	public void updateLimits(double value) {
		min = Math.min(min, value);
		max = Math.max(max, value);
	}

	public Pair<Double, Double> limits() {
		return Pair.of(min, max);
	}

	public String traceName() {
		return traceName;
	}
	
	private String traceName(int index) {
		return traceName() + index;
	}

	// assuming that this is an output parameter
	public List<String> traceNames() {
		final List<String> res = new ArrayList<>();
		for (int j = 0; j < cutoffs.size(); j++) {
			res.add(traceName(j));
		}
		return res;
	}

	// assuming that this is an output parameter
	public List<String> descriptions() {
		final List<String> res = new ArrayList<>();
		for (int j = 0; j < cutoffs.size(); j++) {
			if (cutoffs.size() == 1) {
				res.add("any " + traceName);
			} else if (j == 0) {
				res.add(traceName + " < " + cutoffs.get(j));
			} else if (j == cutoffs.size() - 1) {
				res.add(cutoffs.get(j - 1) + " ≤ " + traceName);
			} else {
				res.add(cutoffs.get(j - 1) + " ≤ " + traceName + " < "
						+ cutoffs.get(j));
			}
		}
		return res;
	}

	public int traceNameIndex(double value) {
		for (int i = 0; i < cutoffs.size(); i++) {
			if (value < cutoffs.get(i)) {
				return i;
			}
		}
		throw new AssertionError();
	}

	public String traceName(double value) {
		return traceName(traceNameIndex(value));
	}

	// assuming that this is an output parameter
	public List<String> actionspec() {
		final List<String> res = new ArrayList<>();
		final List<String> actions = new ArrayList<>();
		for (int i = 0; i < cutoffs.size(); i++) {
			actions.add("action(" + traceName(i) + ")");
		}
		res.add(String.join(" || ", actions));
		for (int i = 0; i < actions.size(); i++) {
			for (int j = i + 1; j < actions.size(); j++) {
				res.add("!" + actions.get(i) + " || !" + actions.get(j));
			}
		}
		return res;
	}

	// assuming that this is an input parameter
	// smooth changes
	public List<String> temporalProperties() {
		final List<String> res = new ArrayList<>();
		if (cutoffs.size() < 3) {
			return res;
		}
		for (int i = 0; i < cutoffs.size(); i++) {
			List<String> vicinity = new ArrayList<>();
			vicinity.add(traceName(i));
			if (i > 0) {
				vicinity.add(traceName(i - 1));
			}
			if (i < cutoffs.size() - 1) {
				vicinity.add(traceName(i + 1));
			}
			vicinity = vicinity.stream().map(s -> "action(" + s + ")")
					.collect(Collectors.toList());
			res.add("G(!" + vicinity.get(0) + " || X("
					+ String.join(" || ", vicinity) + "))");
		}
		return res;
	}
	
	@Override
	public String toString() {
		return "param " + aprosName + " (" + traceName + ") "
				+ cutoffs.subList(0, cutoffs.size() - 1);
	}
}
