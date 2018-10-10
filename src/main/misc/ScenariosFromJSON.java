package main.misc;

import bool.MyBooleanExpression;
import jdk.nashorn.internal.ir.LexicalContext;
import jdk.nashorn.internal.ir.Node;
import jdk.nashorn.internal.ir.PropertyNode;
import jdk.nashorn.internal.ir.visitor.NodeVisitor;
import jdk.nashorn.internal.parser.JSONParser;
import jdk.nashorn.internal.runtime.Source;
import scenario.StringActions;
import scenario.StringScenario;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by buzhinsky on 7/5/16.
 */
public class ScenariosFromJSON {
    public static void main(String[] args) throws IOException {
        final String prefix = "evaluation/moore-json/";
        for (String suffix : Arrays.asList("train", "test")) {
            final String dir = prefix + suffix + "/";
            for (String filename : new File(dir).list()) {
                if (!filename.endsWith(".json")) {
                    continue;
                }
                System.out.println("processing " + dir + filename);
                final List<StringScenario> scenarios = jsonToScenarios(dir + filename);
                try (PrintWriter out = new PrintWriter(dir + filename.replace(".json", ".sc"))) {
                    for (StringScenario sc : scenarios) {
                        out.println(sc);
                        out.println();
                    }
                }
            }
        }
    }

    private static List<StringScenario> jsonToScenarios(String filename) throws IOException {
        final List<StringScenario> result = new ArrayList<>();
        final Node node = null;

        //final Node node = new JSONParser(Source.sourceFor("sc", new File(filename)), null).parse();
        // FIXME

        node.accept(new NodeVisitor<LexicalContext>(new LexicalContext()) {
            List<StringActions> outputs;

            @Override
            public boolean enterPropertyNode(PropertyNode propertyNode) {
                final String wordType = propertyNode.getKeyName();
                final String word = propertyNode.getValue().toString();
                switch (wordType) {
                    case "output-word":
                        outputs = splitList(word).stream().map(t -> binaryEncodeOutput(Integer.parseInt(t)))
                                .collect(Collectors.toList());
                        break;
                    case "input-word":
                        final List<String> inputs = new ArrayList<>();
                        inputs.add("");
                        inputs.addAll(splitList(word).stream().map(t -> "e" + t).collect(Collectors.toList()));
                        result.add(new StringScenario(inputs, Collections.nCopies(inputs.size(),
                                MyBooleanExpression.getTautology()), outputs));
                        break;
                    default:
                        throw new RuntimeException();
                }
                return super.enterPropertyNode(propertyNode);
            }
        });
        return result;
    }

    private static List<String> splitList(String list) {
        return Arrays.asList(list.replace("[", "").replace("\"", "").replace("]", "").split(", "));
    }

    static StringActions binaryEncodeOutput(int output) {
        final List<String> bits = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (((output & (1 << i)) >> i) == 1) {
                bits.add("b" + i);
            }
        }
        return new StringActions(bits);
    }
}
