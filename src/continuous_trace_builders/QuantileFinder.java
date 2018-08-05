package continuous_trace_builders;

/*
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.parameters.Parameter;
import continuous_trace_builders.parameters.RealParameter;
import continuous_trace_builders.parameters.SegmentsParameter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class QuantileFinder {
    public static void run(Configuration conf, String directory, String datasetFilename) throws IOException {
        final Dataset ds = Dataset.load(Utils.combinePaths(directory, datasetFilename));
        for (Parameter p : conf.parameters()) {
            if (p instanceof RealParameter || p instanceof SegmentsParameter) {
                System.out.println(p);
                final Set<Integer> ns = new TreeSet<>();
                for (int i = 2; i <= 10; i++) {
                    ns.add(i);
                }
                final Map<Integer, List<Double>> map = find(ds, p, ns);
                for (int i : ns) {
                    System.out.print(i + "-quantiles:");
                    for (int j = 1; j < i; j++) {
                        System.out.print(" " + Math.round(map.get(i).get(j)));
                    }
                    System.out.println();
                }
                System.out.println();
            }
        }
    }

    public static Map<Integer, List<Double>> find(Dataset ds, Parameter p, Set<Integer> ns) throws IOException {
        final List<Double> values = new ArrayList<>();
        final Dataset.Reader reader = ds.reader();
        while (reader.hasNext()) {
            final List<double[]> trace = reader.next();
            values.addAll(trace.stream().map(aTrace -> ds.get(aTrace, p)).collect(Collectors.toList()));
        }
        Collections.sort(values);
        final Map<Integer, List<Double>> map = new TreeMap<>();
        for (int n : ns) {
            final List<Double> quantiles = new ArrayList<>();
            for (int j = 1; j < n; j++) {
                final int index = values.size() / n * j;
                quantiles.add(values.get(index));
            }
            map.put(n, quantiles);
        }
        return map;
    }
}
