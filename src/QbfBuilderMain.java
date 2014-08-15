import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import qbf.ltl.LtlParseException;
import qbf.ltl.LtlParser;
import qbf.ltl.grammar.LtlNode;
import qbf.reduction.Solvers;
import structures.Automaton;
import structures.ScenariosTree;
import algorithms.QbfAutomatonBuilder;

public class QbfBuilderMain {

	@Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
	private List<String> arguments = new ArrayList<String>();

	@Option(name = "--size", aliases = { "-s" }, usage = "automaton size", metaVar = "<size>", required = true)
	private int size;

	@Option(name = "--log", aliases = { "-l" }, usage = "write log to this file", metaVar = "<file>")
	private String logFilePath;

	@Option(name = "--result", aliases = { "-r" }, usage = "write result automaton in GV format to this file", metaVar = "<GV file>")
	private String resultFilePath;

	@Option(name = "--tree", aliases = { "-t" }, usage = "write scenarios tree in GV format to this file", metaVar = "<GV file>")
	private String treeFilePath;

	@Option(name = "--complete", aliases = { "-c" }, handler = BooleanOptionHandler.class,
			usage = "generate automaton which has a transition for all (event, expression) pairs")
	private boolean isComplete;

	
	// FOR QBF
	@Option(name = "--ltl", aliases = { "-lt" }, usage = "file with LTL properties", metaVar = "<file>")
	private String ltlFilePath;

	@Option(name = "--depth", aliases = { "-d" }, usage = "BMC depth (k)", metaVar = "<depth>")
	private int depth = 0;
	
	@Option(name = "--extractSubterms", aliases = { "-es" }, handler = BooleanOptionHandler.class,
			usage = "whether subterms should be extracted to separate variables", metaVar = "<extractSubterms>")
	private boolean extractSubterms;
	
	@Option(name = "--qbfSolver", aliases = { "-qs" }, usage = "QBF solver (SKIZZO / DEPQBF)", metaVar = "<qbfSolver>")
	private String qbfSolver = "SKIZZO";
	
	@Option(name = "--timeout", aliases = { "-to" }, usage = "QBF solver timeout (sec)", metaVar = "<timeout>")
	private int timeout = 60 * 60 * 24;
		
	
	private void launcher(String[] args) throws IOException {
		Locale.setDefault(Locale.US);

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("TODO description");
			System.out.println("Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)\n");
			System.out.print("Usage: ");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}
		
		try {
			Runtime.getRuntime().exec(Solvers.valueOf(qbfSolver).command);
		} catch (IOException e) {
			System.err.println("ERROR: Problems with solver execution (" + qbfSolver + ")");
			e.printStackTrace();
			return;
		}

		Logger logger = Logger.getLogger("Logger");
		if (logFilePath != null) {
			try {
				FileHandler fh = new FileHandler(logFilePath, false);
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);

				logger.setUseParentHandlers(false);
				System.out.println("Log redirected to " + logFilePath);
			} catch (Exception e) {
				System.err.println("Can't work with file " + logFilePath + ": " + e.getMessage());
				return;
			}
		}

		ScenariosTree tree = new ScenariosTree();
		for (String filePath : arguments) {
			try {
				tree.load(filePath);
				logger.info("Loaded scenarios from " + filePath);
				logger.info("  Total scenarios tree size: " + tree.nodesCount());
			} catch (Exception e) {
				logger.warning("Can't load scenarios from file " + filePath);
				return;
			}
		}

		if (treeFilePath != null) {
			try (PrintWriter treePrintWriter = new PrintWriter(new File(treeFilePath))) {
				treePrintWriter.println(tree);
				logger.info("Scenarios tree saved to " + treeFilePath);
			} catch (Exception e) {
				logger.warning("Can't save scenarios tree to " + treeFilePath);
			}
		}
		
		try {
			List<LtlNode> formulae = LtlParser.loadProperties(ltlFilePath);
			logger.info("LTL formula from " + ltlFilePath);
			
			long startTime = System.currentTimeMillis();
			logger.info("Start building automaton");
			Optional<Automaton> resultAutomaton = QbfAutomatonBuilder.build(logger, tree, formulae, size, depth, timeout,
					Solvers.valueOf(qbfSolver), extractSubterms, isComplete, arguments);
			double executionTime = (System.currentTimeMillis() - startTime) / 1000.;
			
			if (!resultAutomaton.isPresent()) {
				logger.info("Automaton with " + size + " states NOT FOUND!");
			} else {
				logger.info("Automaton with " + size + " states WAS FOUND!");
				if (resultFilePath != null) {
					try (PrintWriter resultPrintWriter = new PrintWriter(new File(resultFilePath))) {
						resultPrintWriter.println(resultAutomaton.get());
					} catch (FileNotFoundException e) {
						logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
					}
				}
			}
			logger.info("Automaton builder execution time: " + executionTime);
		} catch (ParseException | LtlParseException e) {
			logger.warning("Can't get LTL formula from " + treeFilePath);
			throw new RuntimeException(e);
		}
	}

	public void run(String[] args) {
		try {
			launcher(args);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new QbfBuilderMain().run(args);
	}

}