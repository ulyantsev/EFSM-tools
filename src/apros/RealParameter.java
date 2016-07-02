package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class RealParameter extends Parameter {
	final List<Double> cutoffs;
	private int lowerBound = Integer.MIN_VALUE + 1;
	private int upperBound = Integer.MAX_VALUE;
	private final Pair<Double, Double> doubleBounds;
	
	public RealParameter(String aprosName, String traceName, Double... cutoffs) {
		this(aprosName, traceName, Pair.of(-Double.MAX_VALUE, Double.MAX_VALUE), cutoffs);
	}
	
	public RealParameter(String aprosName, String traceName,
			Pair<Double, Double> bounds, Double... cutoffs) {
		super(aprosName, traceName);
		this.doubleBounds = bounds;
		this.cutoffs = new ArrayList<>(Arrays.asList(cutoffs));
		this.cutoffs.add(Double.POSITIVE_INFINITY);
		if (bounds.getLeft() > lowerBound) {
			lowerBound = (int) Math.round(Math.floor(bounds.getLeft()));
		}
		if (bounds.getRight() < upperBound) {
			upperBound = (int) Math.round(Math.ceil(bounds.getRight()));
		}
	}
	
	@Override
	public List<String> traceNames() {
		final List<String> res = new ArrayList<>();
		for (int j = 0; j < cutoffs.size(); j++) {
			res.add(traceName(j));
		}
		return res;
	}

	@Override
	public List<String> descriptions() {
		final List<String> res = new ArrayList<>();
		for (int j = 0; j < cutoffs.size(); j++) {
			final double lower = j == 0 ? doubleBounds.getLeft() : cutoffs.get(j - 1);
			final double upper = j == cutoffs.size() - 1
					? doubleBounds.getRight() : cutoffs.get(j);
			res.add("[" + j + "] " + lower + " ≤ " + traceName() + " < " + upper);
		}
		return res;
	}
	
	@Override
	public int traceNameIndex(double value) {
		if (value < lowerBound || value > upperBound) {
			throw new RuntimeException("Parameter " + traceName()
				+ ": bounds violated for value " + value);
		}
		for (int i = 0; i < cutoffs.size(); i++) {
			if (value < cutoffs.get(i)) {
				return i;
			}
		}
		throw new AssertionError();
	}

	@Override
	public int valueCount() {
		return cutoffs.size();
	}
	
	@Override
	public String toString() {
		final ArrayList<Double> thresholds = new ArrayList<>();
		thresholds.add((double) lowerBound);
		thresholds.addAll(cutoffs.subList(0, cutoffs.size() - 1));
		thresholds.add((double) upperBound);

		return "param " + aprosName() + " (" + traceName() + "): REAL"
				+ thresholds;
	}

	@Override
	public String nusmvType() {
		return lowerBound + ".." + upperBound;
	}

	@Override
	public String nusmvCondition(String name, int index) {
		if (index == 0) {
			return name + " <= " + intervalMax(index);
		} else if (index == valueCount() - 1) {
			return name + " >= " + intervalMin(index);
		} else {
			return name + " in " + nusmvInterval(index);
		}
	}

    private int intervalMin(int interval) {
    	return interval == 0 ? lowerBound
				: (int) Math.round(Math.floor(cutoffs.get(interval - 1)));
    }
    
    private int intervalMax(int interval) {
    	return interval == cutoffs.size() - 1 ? upperBound
				: (int) Math.round(Math.ceil(cutoffs.get(interval)));
    }
    
	@Override
	public String nusmvInterval(int index) {
		return intervalMin(index) + ".." + intervalMax(index);
	}

	@Override
	public String defaultValue() {
		return "0";
	}
}
