package continuous_trace_builders;

/**
 * Created by buzhinsky on 1/3/18.
 */
public class Counter {
    private int counter = 0;

    public void add(int value) {
        counter += value;
    }

    public void log() {
        System.out.println(" generated " + counter + " constraints");
        counter = 0;
    }
}
