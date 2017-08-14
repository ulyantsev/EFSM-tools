package continuous_trace_builders;

import structures.moore.MooreNode;

import java.io.IOException;

/**
 * Created by buzhinsky on 8/12/17.
 */
public interface MooreNodeIterator {
    MooreNode next() throws IOException;
}
