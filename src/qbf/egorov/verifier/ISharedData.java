package qbf.egorov.verifier;

import java.util.Set;

import qbf.egorov.util.concurrent.DfsStackTreeNode;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionNode;

public interface ISharedData {
    DfsStackTreeNode<IIntersectionTransition<?>> getContraryInstance();

    /**
     * Set contrary instance
     * @param contraryInstance contrary instance
     * @return false if contrary instance has been already set.
     */
    boolean setContraryInstance(DfsStackTreeNode<IIntersectionTransition<?>> contraryInstance);

    Set<IntersectionNode<?>> getVisited();
}
