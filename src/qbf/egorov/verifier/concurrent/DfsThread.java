/**
 * DfsThread.java, 09.05.2008
 */
package qbf.egorov.verifier.concurrent;

import java.io.PrintStream;
import java.util.Map;

import qbf.egorov.util.concurrent.DfsStackTree;
import qbf.egorov.verifier.IDfsListener;
import qbf.egorov.verifier.ISharedData;
import qbf.egorov.verifier.automata.IIntersectionTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class DfsThread extends Thread {
    private DfsStackTree<IIntersectionTransition<?>> stackTree;
    private ISharedData sharedData;
    private int threadId;

    private IDfsListener[] listeners = new IDfsListener[0];

    public DfsThread(DfsStackTree<IIntersectionTransition<?>> stackTree, ISharedData sharedData, int threadId) {
        super();
        if (sharedData == null) {
            throw new IllegalArgumentException();
        }
        this.sharedData = sharedData;
        this.stackTree = stackTree;
        this.threadId = threadId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void run() {
        if (stackTree == null) {
            throw new RuntimeException("Initial stack tree node hasn't been initialized yet");
        }
        try {
            ConcurrentMainDfs dfs = new ConcurrentMainDfs(sharedData, stackTree, threadId);

            for (IDfsListener l : listeners) {
                dfs.add(l);
            }

            dfs.dfs(stackTree.getRoot().getItem().getTarget());
        } catch (Throwable t) {
            printAllStacks(System.err);
            t.printStackTrace();
        }
    }

    private static void printAllStacks(PrintStream s) {
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();

        synchronized (s) {
            for (Map.Entry<Thread, StackTraceElement[]> entry: stacks.entrySet()) {
                s.println("Thread " + entry.getKey().getName());
                for (StackTraceElement trace: entry.getValue()) {
                    s.println("\tat " + trace);
                }
            }
            s.println();
        }
    }
}
