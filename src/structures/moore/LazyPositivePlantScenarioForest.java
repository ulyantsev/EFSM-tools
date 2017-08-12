package structures.moore;

import continuous_trace_builders.MooreNodeIterable;
import continuous_trace_builders.MooreNodeIterator;
import org.apache.commons.lang3.tuple.Pair;
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
            private Iterator<MooreNode> curIterator;

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

            private Iterator<MooreNode> newIterator() throws IOException {
                final PositivePlantScenarioForest forest = newLineGraph();
                return forest == null ? null : forest.nodes().iterator();
            }

            @Override
            public Pair<MooreNode, Boolean> next() throws IOException {
                boolean initial = false;
                if (curIterator == null) {
                    curIterator = newIterator();
                    if (curIterator == null) {
                        return null;
                    }
                    initial = true;
                }
                MooreNode nextNode = curIterator.hasNext() ? curIterator.next() : null;
                if (nextNode == null) {
                    curIterator = newIterator();
                    if (curIterator == null) {
                        return null;
                    }
                    nextNode = curIterator.next();
                    initial = true;
                }
                return Pair.of(nextNode, initial);
            }
        };
    }
}
