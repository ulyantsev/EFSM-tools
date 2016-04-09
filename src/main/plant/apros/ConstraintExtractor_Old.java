package main.plant.apros;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConstraintExtractor_Old {
	final static Configuration CONF = TraceTranslator.CONF_REA_TUR_TRIP;
	
	final static boolean OVERALL_1D = true;
	final static boolean OVERALL_2D = true;
	final static boolean INPUT_STATE = true;
	final static boolean CURRENT_NEXT = true;
	
	public static void main(String[] args) throws FileNotFoundException {
		final Dataset ds = new Dataset(CONF.intervalSec, TraceTranslator.INPUT_DIRECTORY);
		final StringBuilder sb = new StringBuilder();
		//CONF.inputParameters
		final String inputLine = String.join(", ",
				CONF.inputParameters.stream().map(p -> "CONT_INPUT_" + p.traceName())
    			.collect(Collectors.toList()));
		sb.append("MODULE PLANT(" + inputLine + ")\n");
    	sb.append("VAR\n");
    	for (Parameter p : CONF.outputParameters) {
    		sb.append("    CONT_" + p.traceName() + ": " + p.nusmvType() + ";\n");
    	}
    	
    	final List<String> initConstraints = new ArrayList<>();
    	final List<String> transConstraints = new ArrayList<>();
    	
    	// 1. overall 1-dimensional constraints
    	if (OVERALL_1D) {
	    	for (Parameter p : CONF.outputParameters) {
	    		final Set<Integer> indices = new TreeSet<>();
	    		for (List<double[]> trace : ds.values) {
	        		for (double[] snapshot : trace) {
						final int index = p.traceNameIndex(ds.get(snapshot, p));
						indices.add(index);
	        		}
	    		}
	    		final String range = " in (" + String.join(" union ",
	    				indices.stream().map(i -> p.nusmvInterval(i))
	    				.collect(Collectors.toList())) + ")";
	    		initConstraints.add("CONT_" + p.traceName() + range);
	    		transConstraints.add("next(CONT_" + p.traceName() + ")" + range);
			}
    	}
    	// 2. overall 2-dimensional constraints
    	if (OVERALL_2D) {
	    	for (int i = 0; i < CONF.outputParameters.size(); i++) {
				final Parameter pi = CONF.outputParameters.get(i);
	    		for (int j = 0; j < i; j++) {
	    			final Parameter pj = CONF.outputParameters.get(j);
		    		final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
	        		for (List<double[]> trace : ds.values) {
	            		for (double[] snapshot : trace) {
	    					final int index1 = pi.traceNameIndex(ds.get(snapshot, pi));
	    					final int index2 = pj.traceNameIndex(ds.get(snapshot, pj));
	    					Set<Integer> secondIndices = indexPairs.get(index1);
							if (secondIndices == null) {
								secondIndices = new TreeSet<>();
								indexPairs.put(index1, secondIndices);
							}
							secondIndices.add(index2);
	            		}
	        		}
	        		for (List<String> list : Arrays.asList(initConstraints, transConstraints)) {
	        			final Function<Parameter, String> varName = p -> {
	        				String res = "CONT_" + p.traceName();
	        				if (list == transConstraints) {
	        					res = "next(" + res + ")";
	        				}
	        				return res;
	        			};
	        			
	        			final List<String> optionList = new ArrayList<>();
	    	    		for (Map.Entry<Integer, Set<Integer>> implication : indexPairs.entrySet()) {
	    	    			final int index1 = implication.getKey();
	    		    		final List<String> options = new ArrayList<>();
	    	    			for (int index2 : implication.getValue()) {
	    	    				options.add(pj.nusmvInterval(index2));
	    	    			}
	    	    			optionList.add(varName.apply(pi)
	    						+ " in " + pi.nusmvInterval(index1)
	    						+ " & " + varName.apply(pj)
	    						+ " in ("
	    						+ String.join(" union ", options) + ")");
	    	    		}
	    	    		list.add(String.join(" | ", optionList));
	        		}
	        	}
	    	}
    	}

    	// 2. 2-dimensional constraints "input -> possible next state"
    	// if the input is unknown, then no constraint
    	if (INPUT_STATE) {
    		for (Parameter pi : CONF.inputParameters) {
	    		for (Parameter po : CONF.outputParameters) {
		    		final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
		    		for (int index1 = 0; index1 < pi.valueCount(); index1++) {
		    			indexPairs.put(index1, new TreeSet<>());
		    		}
		    		for (List<double[]> trace : ds.values) {
		        		for (int i = 0; i < trace.size() - 1; i++) {
		        			final int index1 = pi.traceNameIndex(ds.get(trace.get(i), pi));
							final int index2 = po.traceNameIndex(ds.get(trace.get(i + 1), po));
							indexPairs.get(index1).add(index2);
		        		}
		    		}
					final List<String> optionList = new ArrayList<>();
					for (int index1 = 0; index1 < pi.valueCount(); index1++) {
			    		final List<String> options = new ArrayList<>();
		    			for (int index2 : indexPairs.get(index1)) {
		    				options.add(po.nusmvInterval(index2));
		    			}
		    			optionList.add("CONT_INPUT_" + pi.traceName()
							+ " in " + pi.nusmvInterval(index1)
							+ (options.isEmpty() ? "" : (" & next(CONT_"
							+ po.traceName() + ") in ("
							+ String.join(" union ", options) + ")")));
		    		}
					transConstraints.add(String.join(" | ", optionList));
	    		}
    		}
    	}
    	
    	// 2. 2-dimensional constraints "current state -> next state"
    	if (CURRENT_NEXT) {
    		for (Parameter p : CONF.outputParameters) {
	    		final Map<Integer, Set<Integer>> indexPairs = new TreeMap<>();
	    		for (List<double[]> trace : ds.values) {
	        		for (int i = 0; i < trace.size() - 1; i++) {
	        			final int index1 = p.traceNameIndex(ds.get(trace.get(i), p));
						final int index2 = p.traceNameIndex(ds.get(trace.get(i + 1), p));
						Set<Integer> secondIndices = indexPairs.get(index1);
						if (secondIndices == null) {
							secondIndices = new TreeSet<>();
							indexPairs.put(index1, secondIndices);
						}
						secondIndices.add(index2);
	        		}
	    		}
				final List<String> optionList = new ArrayList<>();
	    		for (Map.Entry<Integer, Set<Integer>> implication : indexPairs.entrySet()) {
	    			final int index1 = implication.getKey();
		    		final List<String> options = new ArrayList<>();
	    			for (int index2 : implication.getValue()) {
	    				options.add(p.nusmvInterval(index2));
	    			}
	    			optionList.add("CONT_" + p.traceName()
						+ " in " + p.nusmvInterval(index1)
						+ " & next(CONT_" + p.traceName()
						+ ") in ("
						+ String.join(" union ", options) + ")");
	    		}
				transConstraints.add(String.join(" | ", optionList));
    		}
    	}
    	
    	sb.append("INIT\n");
    	if (initConstraints.isEmpty()) {
        	initConstraints.add("TRUE");
    	}
    	sb.append("    (" + String.join(")\n  & (", initConstraints) + ")\n");
    	sb.append("TRANS\n");
    	if (transConstraints.isEmpty()) {
    		transConstraints.add("TRUE");
    	}
    	sb.append("    (" + String.join(")\n  & (", transConstraints) + ")\n");
		System.out.println(sb);
	}
}
