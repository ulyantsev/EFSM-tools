package continuous_trace_builders;

import org.apache.commons.lang3.tuple.Pair;
import structures.moore.MooreNode;

import java.io.IOException;

/**
 * Created by buzhinsky on 8/12/17.
 */
public interface MooreNodeIterator {
    Pair<MooreNode, Boolean> next() throws IOException;
}
