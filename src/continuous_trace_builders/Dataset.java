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
    private int totalFiles = 0;
    private int totalTraces = 0;
    private int maxTraceLength = 0;
    private int minTraceLength = Integer.MAX_VALUE;
    private String dirName;

    int totalTraces() {
        return totalTraces;
    }

    int totalFiles() {
        return totalFiles;
    }

    int maxTraceLength() {
        return maxTraceLength;
    }

    int minTraceLength() {
        return minTraceLength;
    }

    public final static long serialVersionUID = 1L;

    public static Dataset load(String dirName) throws IOException {
        final Dataset ds;
        final String inFilename = Utils.combinePaths(dirName, "header.bin");
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inFilename))) {
            ds = (Dataset) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
        ds.dirName = dirName;
        return ds;
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

    private static final int MAX_VALUES_IN_FILE = 5_000_000;

    public Reader reader() throws IOException {
        return new Reader();
    }

    public class Reader {
        List<List<double[]>> currentValues;
        int currentFile = 0;
        int currentTraceInFile;

        Reader() throws IOException {
            read();
        }

        @SuppressWarnings("unchecked")
        void read() throws IOException {
            final String inFilename = Utils.combinePaths(dirName, currentFile + ".bin");
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inFilename))) {
                currentValues = (List<List<double[]>>) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException();
            }
            currentTraceInFile = 0;
        }

        public boolean hasNext() {
            return currentFile < totalFiles - 1 || currentTraceInFile < currentValues.size();
        }

        public List<double[]> next() throws IOException {
            if (hasNext()) {
                if (currentTraceInFile == currentValues.size()) {
                    read();
                    currentFile++;
                }
                return currentValues.get(currentTraceInFile++);
            }
            throw new NoSuchElementException();
        }
    }

    private class Writer {
        int valuesWritten = 0;
        List<List<double[]>> currentValues = new ArrayList<>();

        void write(List<double[]> trace) throws IOException {
            totalTraces++;
            maxTraceLength = Math.max(maxTraceLength, trace.size());
            minTraceLength = Math.max(minTraceLength, trace.size());

            final int newValueNumber = trace.get(0).length * trace.size();
            if (valuesWritten + newValueNumber > MAX_VALUES_IN_FILE) {
                writeFile();
            }
            currentValues.add(trace);
            valuesWritten += newValueNumber;
        }

        void writeFile() throws IOException {
            final String outFilename = Utils.combinePaths(dirName, totalFiles + ".bin");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFilename))) {
                out.writeObject(currentValues);
            }
            valuesWritten = 0;
            currentValues.clear();
            totalFiles++;
        }

        void finish() throws IOException {
            writeFile();
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

        final String outFilename = Utils.combinePaths(outputDirName, "header.bin");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFilename))) {
            out.writeObject(this);
        }
    }
}
