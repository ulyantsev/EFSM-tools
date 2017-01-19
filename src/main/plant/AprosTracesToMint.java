package main.plant;

import apros.Configuration;
import apros.Dataset;
import apros.Parameter;
import apros.TraceTranslator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

/**
 * (c) Igor Buzhinsky
 */

public class AprosTracesToMint {
    public static void main(String[] args) throws IOException {
        final int traceIncludeEach = 25;
        final Configuration conf = Configuration.load("evaluation/apros-configurations/s1.conf");
        final Dataset ds = Dataset.load("evaluation/apros-scripts/dataset_.bin");
        try (PrintWriter traceWriter = new PrintWriter(new File("evaluation/plant-synthesis/npp.mint-trace"))) {
            traceWriter.println("types");

            final Set<String> allEvents = new TreeSet<>();
            final char[] arr = new char[conf.inputParameters.size() + 1];
            arr[0] = 'A';
            TraceTranslator.allEventCombinations(arr, 1, allEvents, conf.inputParameters);

            for (String e : allEvents) {
                traceWriter.print(e);
                for (Parameter p : conf.outputParameters) {
                    traceWriter.print(" " + p.traceName() + ":N");
                }
                traceWriter.println();
            }

            for (int i = 0; i < ds.values.size(); i++) {
                if (i % traceIncludeEach != 0) {
                    continue;
                }
                traceWriter.println("trace");
                for (double[] values : ds.values.get(i)) {
                    final StringBuilder event = new StringBuilder("A");
                    for (Parameter p : conf.inputParameters) {
                        event.append(p.traceNameIndex(ds.get(values, p)));
                    }
                    traceWriter.print(event);
                    for (Parameter p : conf.outputParameters) {
                        traceWriter.print(" " + ds.get(values, p));
                    }
                    traceWriter.println();
                }
            }
        }
    }

    /*public static void main(String[] args) throws IOException {
        final int traceIncludeEach = 25;
        final Configuration conf = Configuration.load("evaluation/apros-configurations/s1.conf");
        final Dataset ds = Dataset.load("evaluation/apros-scripts/dataset_.bin");
        try (PrintWriter traceWriter = new PrintWriter(new File("evaluation/plant-synthesis/npp.mint-trace"))) {
            traceWriter.println("types");
            traceWriter.print("evt");
            for (Parameter p : conf.outputParameters) {
                traceWriter.print(" " + p.traceName() + ":N");
            }
            for (Parameter p : conf.inputParameters) {
                traceWriter.print(" " + p.traceName() + ":N");
            }
            traceWriter.println();
            for (int i = 0; i < ds.values.size(); i++) {
                if (i % traceIncludeEach != 0) {
                    continue;
                }
                traceWriter.println("trace");
                for (double[] values : ds.values.get(i)) {
                    traceWriter.print("evt");
                    for (Parameter p : conf.outputParameters) {
                        traceWriter.print(" " + ds.get(values, p));
                    }
                    for (Parameter p : conf.inputParameters) {
                        traceWriter.print(" " + ds.get(values, p));
                    }
                    traceWriter.println();
                }
            }
        }
    }*/
}