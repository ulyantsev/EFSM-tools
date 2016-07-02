package sat_solving;

import java.io.IOException;
import java.util.List;

/**
 * Created by buzhinsky on 7/2/16.
 */

public interface SolverInterface {
    void halt() throws IOException;
    SolverResult solve(List<int[]> newConstraints, int timeLeftForSolver) throws IOException;
}
