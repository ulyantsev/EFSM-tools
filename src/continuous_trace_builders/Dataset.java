package continuous_trace_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.parameters.Parameter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class Dataset implements Serializable {
    private final Map<String, Integer> paramIndices = new HashMap<>();
    private final Map<String, Double> paramScales;
    private int totalTraces = 0;
    private int totalElements = 0;
    private int minTraceLength = Integer.MAX_VALUE;
    private int maxTraceLength = 0;
    private String dirName;

    private final static String HEADER_FILENAME = "header.bin";
    private final static String DATA_FILENAME = "data.txt";

    @Override
    public String toString() {
        return "Dataset [dir = " + dirName+ ", #traces = " + totalTraces() + ", #elements = " + totalElements()
                + ", minTraceLength = " + minTraceLength() + ", maxTraceLength = " + maxTraceLength() + "]";
    }

    public int totalTraces() {
        return totalTraces;
    }

    public int totalElements() {
        return totalElements;
    }

    public int minTraceLength() {
        return minTraceLength;
    }

    public int maxTraceLength() {
        return maxTraceLength;
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
                traces.get(i).clear();
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
        private final BufferedWriter out;

        Writer() throws IOException {
            out = new BufferedWriter(new FileWriter(Utils.combinePaths(dirName, DATA_FILENAME)));
        }

        void write(List<double[]> trace) throws IOException {
            totalTraces++;
            totalElements += trace.size();
            minTraceLength = Math.min(minTraceLength, trace.size());
            maxTraceLength = Math.max(maxTraceLength, trace.size());
            for (double[] arr : trace) {
                out.write(arrayToString(arr));
                out.newLine();
            }
            out.newLine();
        }

        private String doubleToString(double x) {
            final String value = Double.toString(x);
            return value.endsWith(".0") ? value.substring(0, value.length() - 2) : value;
        }

        private String arrayToString(double[] arr) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                sb.append(doubleToString(arr[i]));
                if (i != arr.length - 1) {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }

        void finish() throws IOException {
            out.close();
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

        final String[] filenames = new File(traceLocation).list();
        Arrays.sort(filenames);

        for (String filename : filenames) {
            final boolean isArchive = filename.endsWith(".txt.zip");
            if (!(filename.endsWith(".txt") || isArchive) || !filename.startsWith(traceFilenamePrefix)) {
                continue;
            }
            double timestampToRecord = includeFirstElement? 0 : intervalSec;

            final File file = new File(Utils.combinePaths(traceLocation, filename));

            final InputStream is;
            if (isArchive) {
                final ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
                zis.getNextEntry();
                is = zis;
            } else {
                is = new FileInputStream(file);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                final List<double[]> trace = new ArrayList<>();

                final int paramNum = Integer.valueOf(reader.readLine()) - 1;
                reader.readLine();
                if (paramIndices.isEmpty()) {
                    // read param names
                    for (int i = 0; i < paramNum; i++) {
                        final String[] tokens = Utils.splitString(reader.readLine());
                        final String name = String.join("#", tokens);
                        paramIndices.put(name, paramIndices.size());
                    }
                } else {
                    // skip param names
                    for (int i = 0; i < paramNum; i++) {
                        reader.readLine();
                    }
                }

                String line;
                while ((line = reader.readLine()) != null) {
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
