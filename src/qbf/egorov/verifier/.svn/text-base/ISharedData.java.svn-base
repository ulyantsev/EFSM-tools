package ru.ifmo.verifier;

import ru.ifmo.verifier.automata.IntersectionNode;
import ru.ifmo.verifier.automata.IIntersectionTransition;
import ru.ifmo.verifier.concurrent.DfsThread;
import ru.ifmo.util.concurrent.DfsStackTreeNode;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ISharedData {
    DfsStackTreeNode<IIntersectionTransition> getContraryInstance();

    /**
     * Set contrary instance
     * @param contraryInstance contrary instance
     * @return false if contrary instance has been already set.
     */
    boolean setContraryInstance(DfsStackTreeNode<IIntersectionTransition> contraryInstance);

    Set<IntersectionNode> getVisited();

    /**
     * Insert thread into queue
     * @param t thread to be put into the queue
     * @return false if all threads are waiting.
     */
    boolean offerUnoccupiedThread(DfsThread t);

    /**
     * Get unoccupied thread and remove it from waiting threads
     * @return unoccupied thread or null, if no waiting thread is exist
     */
    DfsThread getUnoccupiedThread();

    void notifyAllUnoccupiedThreads();
}
