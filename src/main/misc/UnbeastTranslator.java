package main.misc;

import scenario.StringScenario;
import verification.ltl.LtlParseException;
import verification.ltl.LtlParser;
import verification.ltl.grammar.LtlNode;
import verification.ltl.grammar.LtlUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by buzhinsky on 4/17/17.
 */
public class UnbeastTranslator {
    public static void main(String[] args) throws LtlParseException, FileNotFoundException, ParseException {
        if (args.length != 5) {
            System.err.println("Usage: java -jar unbeast-translator.jar <scenario path> <ltl path>" +
                    " <comma-separated events> <comma-separated actions> <output path>");
            return;
        }
        final String scenarioPath = args[0];
        final String ltlPath = args[1];
        final List<String> events = Arrays.asList(args[2].split(", *"));
        final List<String> actions = Arrays.asList(args[3].split(", *"));
        final String outputPath = args[4];

        final List<StringScenario> scenarios = StringScenario.loadScenarios(scenarioPath, false);
        final List<String> formulae = LtlParser.load(ltlPath, 0, events).stream()
                .map(LtlUtils::expandEventList).collect(Collectors.toList());
        final List<LtlNode> nodes = LtlParser.parse(formulae);
        final List<String> specification = new ArrayList<>();
        specification.addAll(UnbeastTransformer.Generator.ltlSpecification(nodes));
        specification.addAll(UnbeastTransformer.Generator.scenarioSpecification(scenarios, actions));
        final String problem = UnbeastTransformer.Generator.problemDescription(events, actions, specification);
        try (PrintWriter pw = new PrintWriter(new File(outputPath))) {
            pw.println(problem);
        }
    }

}
