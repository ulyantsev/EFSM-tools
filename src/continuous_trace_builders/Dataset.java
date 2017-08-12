package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.parameters.Parameter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Dataset implements Serializable {
    private final Map<String, Integer> paramIndices = new HashMap<>();
    private final Map<String, Double> paramScales;
    private int totalTraces = 0;
    private int maxTraceLength = 0;
    private int minTraceLength = Integer.MAX_VALUE;
    private String dirName;

    private final static String HEADER_FILENAME = "header.bin";
    private final static String DATA_FILENAME = "data.txt";

    public int totalTraces() {
        return totalTraces;
    }

    public int maxTraceLength() {
        return maxTraceLength;
    }

    public int minTraceLength() {
        return minTraceLength;
    }

    public final static long serialVersionUID = 1L;

    public static Dataset load(String dirName) throws IOException {
        final String inFilename = Utils.combinePaths(dirName, HEADER_FILENAME);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inFilename))) {
            final Dataset ds = (Dataset) in.readObject();
            ds.dirName = dirName;
            return ds;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    Map<Parameter, int[][]> toParamIndices(Collection<Parameter> ps) throws IOException {
        final Map<Parameter, int[][]> res = new HashMap<>();
        for (Parameter p : ps) {
            final List<List<Integer>> traces = new ArrayList<>();
            final Reader reader = new Reader();
            while (reader.hasNext()) {
                traces.add(reader.next().stream().map(line -> p.traceNameIndex(get(line, p)))
                        .collect(Collectors.toList()));
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
        final Integer index = paramIndices.get(p.simulationEnvironmentName());
        if (index == null) {
            throw new RuntimeException("Missing parameter: " + p.simulationEnvironmentName());
        }
        // possible scaling
        final Double oScale = paramScales.get(p.simulationEnvironmentName());
        final double scale = oScale == null? 1.0 : oScale;

        final double result = values[paramIndices.get(p.simulationEnvironmentName())] * scale;
        p.updateLimits(result);
        return result;
    }

    public Reader reader() throws IOException {
        return new Reader();
    }

    public class Reader {
        private final BufferedReader reader;
        private List<double[]> preparedNext;

        Reader() throws IOException {
            reader = new BufferedReader(new FileReader(Utils.combinePaths(dirName, DATA_FILENAME)));
        }

        public boolean hasNext() throws IOException {
            preparedNext = next();
            return preparedNext != null;
        }

        public List<double[]> next() throws IOException {
            final List<double[]> trace;
            if (preparedNext != null) {
                trace = preparedNext;
                preparedNext = null;
            } else {
                String line;
                trace = new ArrayList<>();
                while (true) {
                    line = reader.readLine();
                    if (line == null) {
                        reader.close();
                        return null;
                    }
                    if (line.isEmpty()) {
                        break;
                    }
                    trace.add(Arrays.stream(line.split(" ")).mapToDouble(Double::parseDouble).toArray());
                }
            }
            return trace;
        }
    }

    private class Writer {
        private final PrintWriter pw;

        Writer() throws IOException {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(Utils.combinePaths(dirName, DATA_FILENAME))));
        }

        void write(List<double[]> trace) throws IOException {
            totalTraces++;
            maxTraceLength = Math.max(maxTraceLength, trace.size());
            minTraceLength = Math.min(minTraceLength, trace.size());
            for (double[] arr : trace) {
                pw.println(Arrays.toString(arr).replaceAll("[\\[\\],]", ""));
            }
            pw.println();
        }

        void finish() throws IOException {
            pw.close();
        }
    }

    public Dataset(double intervalSec, String traceLocation, String traceFilenamePrefix,
                   Map<String, Double> paramScales, boolean includeFirstElement, String outputDirName)
            throws IOException {
        this.paramScales = paramScales;
        this.dirName = outputDirName;
        final File dir = new File(dirName);
        if (dir.exists() && (!Utils.deleteDir(dir) || !dir.mkdir()) || !dir.exists() && !dir.mkdir()) {
            throw new IOException("Unable to prepare directory!");
        }
        final Writer writer = new Writer();

        for (String filename : new File(traceLocation).list()) {
            if (!filename.endsWith(".txt") || !filename.startsWith(traceFilenamePrefix)) {
                continue;
            }
            double timestampToRecord = includeFirstElement? 0 : intervalSec;

            try (Scanner sc = new Scanner(new File(Utils.combinePaths(traceLocation, filename)))) {
                final List<double[]> trace = new ArrayList<>();

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

                    final double[] traceElement = new double[paramNum];
                    for (int i = 0; i < paramNum; i++) {
                        traceElement[i] = Double.parseDouble(tokens[i + 1]);
                    }
                    trace.add(traceElement);
                }
                writer.write(trace);
            }
        }

        writer.finish();

        final String outFilename = Utils.combinePaths(outputDirName, HEADER_FILENAME);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFilename))) {
            out.writeObject(this);
        }
    }
}
