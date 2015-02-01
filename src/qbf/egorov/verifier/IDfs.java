package qbf.egorov.verifier;

import qbf.egorov.verifier.automata.IntersectionNode;

public interface IDfs<R> {
    R dfs(IntersectionNode node);
    void add(IDfsListener listener);
}
