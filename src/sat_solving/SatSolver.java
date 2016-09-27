package sat_solving;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * (c) Igor Buzhinsky
 */

public enum SatSolver {
    CRYPTOMINISAT(false, "cryptominisat4 --maxtime="),
    LINGELING(false, "lingeling -t "),
    INCREMENTAL_CRYPTOMINISAT(true, "incremental-cryptominisat-binary ");

    public final boolean isIncremental;
    public final String command;

    SatSolver(boolean isIncremental, String command) {
        this.isIncremental = isIncremental;
        this.command = command;
    }

    public SolverInterface createInterface(List<int[]> positiveConstraints, String actionspec, Logger logger)
            throws IOException {
        switch (this) {
            case INCREMENTAL_CRYPTOMINISAT:
                return new IncrementalInterface(positiveConstraints, actionspec, logger, this);
            case LINGELING: case CRYPTOMINISAT:
                return new RestartInterface(positiveConstraints, actionspec, logger, this);
            default:
                throw new AssertionError();
        }
    }
}
