package main.plant.apros;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TraceModelGenerator {
	final static Configuration CONF = Settings.CONF;
	final static String FILENAME_PREFIX = "correct_recorded_";

	public static void main(String[] args) throws FileNotFoundException {		
		final Dataset ds = new Dataset(CONF.intervalSec,
				TraceTranslator.INPUT_DIRECTORY, FILENAME_PREFIX, TraceTranslator.PARAM_SCALES);
		
		final StringBuilder sb = new StringBuilder();
		final int maxLength = ds.values.stream().mapToInt(v -> v.size()).max().getAsInt();
		final int minLength = ds.values.stream().mapToInt(v -> v.size()).min().getAsInt();
		if (maxLength != minLength) {
			throw new AssertionError();
			// ASSUMES THAT EACH TRACE HAS EQUAL LENGTH
		}
		
		sb.append(ConstraintExtractor.plantCaption(CONF));
		sb.append("    step: 0.." + (maxLength - 1) + ";\n");
		sb.append("    in_loop: boolean;\n");
		sb.append("FROZENVAR\n    trace: 0.." + (ds.values.size() - 1) + ";\n");
		sb.append("ASSIGN\n");
		sb.append("    init(step) := 0;\n");
		sb.append("    next(step) := step < " + (maxLength - 1)
				+ " ? step + 1 : " + (maxLength - 1) + ";\n");
		sb.append("    init(in_loop) := FALSE;\n");
		sb.append("    next(in_loop) := step = " + (maxLength - 1) + ";\n");
		for (Parameter p : CONF.outputParameters) {
			sb.append("    output_" + p.traceName() + " := case\n");
			for (int traceIndex = 0; traceIndex < ds.values.size(); traceIndex++) {
				sb.append("        trace = " + traceIndex + ": case\n");
				final List<Set<Integer>> valuesToSteps = new ArrayList<>();
				for (int i = 0; i < p.valueCount(); i++) {
					valuesToSteps.add(new TreeSet<>());
				}
				for (int step = 0; step < ds.values.get(traceIndex).size(); step++) {
					final double value = ds.get(ds.values.get(traceIndex).get(step), p);
					final int res = p.traceNameIndex(value);
					valuesToSteps.get(res).add(step);
				}
				for (int i = 0; i < p.valueCount(); i++) {
					if (!valuesToSteps.get(i).isEmpty()) {
						sb.append("            step in "
								+ valuesToSteps.get(i).toString().replace("[", "{ ").replace("]", " }")
								+ ": " + i + ";\n");
					}
				}
				sb.append("        esac;\n");
			}
			sb.append("    esac;\n");
		}
		
		sb.append(ConstraintExtractor.plantConversions(CONF));
		
		try (PrintWriter pw = new PrintWriter("trace-model.smv")) {
			pw.println(sb);
		}
		System.out.println("Done.");
	}
}
