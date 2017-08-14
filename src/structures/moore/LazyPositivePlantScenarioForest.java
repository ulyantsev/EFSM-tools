package structures.moore;

import continuous_trace_builders.MooreNodeIterable;
import continuous_trace_builders.MooreNodeIterator;
import scenario.StringScenario;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Iterator;

/**
 * Created by buzhinsky on 8/12/17.
 */
public class LazyPositivePlantScenarioForest implements MooreNodeIterable {
    private final String filename;
    private final boolean removeVars;

    public LazyPositivePlantScenarioForest(String filename, boolean removeVars) {
        this.filename = filename;
        this.removeVars = removeVars;
    }

    @Override
    public MooreNodeIterator nodeIterator() {
        return new MooreNodeIterator() {
            private BufferedReader reader;

            PositivePlantScenarioForest newLineGraph() throws IOException {
                if (reader == null) {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
                }
                try {
                    String inp = "";
                    while (true) {
                        String s = reader.readLine();
                        if (s == null) {
                            reader.close();
                            return null;
                        }
                        if (removeVars) {
                            s = StringScenario.removeVariables(s);
                        }
                        if (inp.isEmpty() && s.isEmpty()) {
                            continue;
                        }
                        if (inp.isEmpty()) {
                            inp = s;
                        } else {
                            final PositivePlantScenarioForest forest = new PositivePlantScenarioForest(true);
                            forest.addScenario(new StringScenario(inp, s));
                            return forest;
                        }
                    }
                } catch (ParseException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public MooreNode next() throws IOException {
                final PositivePlantScenarioForest forest = newLineGraph();
                return forest == null ? null : forest.nodes().iterator().next();
            }
        };
    }
}
