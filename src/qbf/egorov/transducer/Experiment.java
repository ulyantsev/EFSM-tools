/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.io.FileFormatException;

/**
 * @author kegorov
 *         Date: Jun 26, 2009
 */
public class Experiment {
	
	@Argument(usage = "path to specification file", metaVar = "file", required = true)
    private String argument;
	
	@Option(name = "--max_evals", aliases = {"-e"}, usage = "max allowed number of fitness evaluations", metaVar = "<maxEvals>", required=false)
	private int maxFitnessEvals = Integer.MAX_VALUE;
	
	@Option(name = "--max_time", aliases = {"-t"}, usage = "max allowed run time", metaVar = "<maxTime>", required=false)
	private int maxRunTime = Integer.MAX_VALUE;
	
	@Option(name = "--nexpr", aliases = {"-n"}, usage = "number of experiments", metaVar = "<nExperiments>", required=false)
	private int numberOfExperiments = 1;
	
	@Option(name = "--bfs", aliases = {"-b"}, usage = "use BFS cache", handler=BooleanOptionHandler.class, required=false)
	private boolean useBfsCache = false;
	
	@Option(name = "--bfs_cache_size", aliases = {"-s"}, usage = "max BFS cache size")
	private int bfsCacheSize;
	
	@Option(name = "--lazy", aliases = {"-l"}, usage = "use lazy fitness evaluation", handler=BooleanOptionHandler.class, required=false)
	private boolean useLazyFitnessEvaluation = false;
	
	@Option(name = "--nthreads", aliases = {"-nt"}, usage = "number of threads", required=false)
	private int numberOfThreads = 1;
	
	@Option(name = "--seed", aliases = {"-r"}, usage = "random seed", required=false)
	private int seed;

	
    private Writer out;
    private int i;

    public Experiment() {
    }

    private void launch(String args[]) throws IOException, FileFormatException {
    }
    
    public static void main(String[] args) throws FileFormatException, IOException, LtlParseException {
    	new Experiment().launch(args);
    }

    protected void goToLastResult(File f) throws IOException {
        if (!f.exists()) {
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(f));
        try {
            //count line numbers, don't check format
            for (i = 0; reader.readLine() != null; i++);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }

    protected void printResult(FST best) {
    }

    public int getStep() {
        return i;
    }

    public void run() {
    }

    public void close() throws IOException {
        out.close();
    }
}
