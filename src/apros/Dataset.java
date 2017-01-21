package apros;

/**
 * (c) Igor Buzhinsky
 */

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Dataset implements Serializable {
    private final Map<String, Integer> paramIndices = new HashMap<>();
    public final List<List<double[]>> values = new ArrayList<>();
    final Map<String, Double> paramScales;

    public final static long serialVersionUID = 1L;

    public static Dataset load(String filename) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (Dataset) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    public Map<Parameter, int[][]> toParamIndices(Collection<Parameter> ps) {
        final Map<Parameter, int[][]> res = new HashMap<>();
        for (Parameter p : ps) {
            final List<List<Integer>> traces = new ArrayList<>();
            for (List<double[]> trace : values) {
                traces.add(trace.stream().map(line -> p.traceNameIndex(get(line, p))).collect(Collectors.toList()));
            }
            final int[][] tracesM = new int[traces.size()][];
            for (int i = 0; i < tracesM.length; i++) {
                tracesM[i] = new int[traces.get(i).size()];
                for (int j = 0; j < tracesM[i].length; j++) {
                    tracesM[i][j] = traces.get(i).get(j);
                }
            }
            res.put(p, tracesM);
        }
        return res;
    }

    public double get(double[] values, Parameter p) {
        final Integer index = paramIndices.get(p.aprosName());
        if (index == null) {
            throw new RuntimeException("Missing parameter: " + p.aprosName());
        }
        // possible scaling
        final Double oScale = paramScales.get(p.aprosName());
        final double scale = oScale == null? 1.0 : oScale;

        final double result = values[paramIndices.get(p.aprosName())] * scale;
        p.updateLimits(result);
        return result;
    }

    public Dataset(double intervalSec, String traceLocation, String traceFilenamePrefix,
                   Map<String, Double> paramScales, boolean includeFirstElement) throws FileNotFoundException {
        this.paramScales = paramScales;
        for (String filename : new File(traceLocation).list()) {
            if (!filename.endsWith(".txt") || !filename.startsWith(traceFilenamePrefix)) {
                continue;
            }
            double timestampToRecord = includeFirstElement? 0 : intervalSec;

            try (Scanner sc = new Scanner(new File(traceLocation + "/" + filename))) {
                final List<double[]> valueLines = new ArrayList<>();
                values.add(valueLines);

                final int paramNum = Integer.valueOf(sc.nextLine()) - 1;
                sc.nextLine();
                if (paramIndices.isEmpty()) {
                    // read param names
                    for (int i = 0; i < paramNum; i++) {
                        final String[] tokens = Utils.splitString(sc.nextLine());
                        final String name = String.join("#", tokens);
                        paramIndices.put(name, paramIndices.size());
                    }
                } else {
                    // skip param names
                    for (int i = 0; i < paramNum; i++) {
                        sc.nextLine();
                    }
                }

                while (sc.hasNextLine()) {
                    final String line = sc.nextLine();
                    final String[] tokens = Utils.splitString(line);

                    double curTimestamp = Double.parseDouble(tokens[0]);
                    if (curTimestamp >= timestampToRecord) {
                        timestampToRecord += intervalSec;
                    } else {
                        continue;
                    }

                    final double[] valueLine = new double[paramNum];
                    for (int i = 0; i < paramNum; i++) {
                        valueLine[i] = Double.parseDouble(tokens[i + 1]);
                    }
                    valueLines.add(valueLine);
                }
            }
        }
    }
}
