package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringScenario;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;

public class WaterLevelGeneratorMint {
    private static String actionsToMint(String[] actions) {
        final double level;
        switch (actions[0]) {
            case "belowll":
                level = 0.1;
                break;
            case "belowl":
                level = 0.3;
                break;
            case "belowth":
                level = 0.4;
                break;
            case "belowsp":
                level = 0.475;
                break;
            case "abovesp":
                level = 0.525;
                break;
            case "aboveth":
                level = 0.6;
                break;
            case "aboveh":
                level = 0.7;
                break;
            case "abovehh":
                level = 0.9;
                break;
            default:
                throw new RuntimeException();
        }
        final boolean dry = actions[1].equals("sensordry");
        return level + " " + dry;
    }
    
    public static void main(String[] args) throws IOException, ParseException {
        for (int k = 1; k <= 10; k++) {
            final String dir = "evaluation/plant-synthesis/";
            final String prefix = dir + "water-level-" + k;
            final List<StringScenario> scs = StringScenario.loadScenarios(prefix + ".sc", false);

            try (PrintWriter traceWriter = new PrintWriter(new File(prefix + ".mint-trace"))) {
                traceWriter.println("types");
                traceWriter.println("init level:N dry:S");
                traceWriter.println("open level:N dry:S");
                traceWriter.println("closed level:N dry:S");
                for (StringScenario sc : scs) {
                    traceWriter.println("trace");
                    for (int i = 0; i < sc.size(); i++) {
                        final String event = sc.getEvents(i).get(0);
                        traceWriter.println((event.isEmpty() ? "init" : event)
                                + " " + actionsToMint(sc.getActions(i).getActions()));
                    }
                }
            }
        }
    }
}
