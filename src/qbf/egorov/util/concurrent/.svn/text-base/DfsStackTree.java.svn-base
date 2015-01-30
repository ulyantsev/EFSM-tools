/*
 * DfsStackTree.java, 04.06.2008
 */
package ru.ifmo.util.concurrent;

public class DfsStackTree<E> {
    private DfsStackTreeNode<E> root;
    private int threadNumber;

    public DfsStackTree(E rootElement, int threadNumber) {
        root = new DfsStackTreeNode<E>(rootElement, threadNumber);
        this.threadNumber = threadNumber;
    }

    public DfsStackTreeNode<E> getRoot() {
        return root;
    }

    /**
     * Add children with item <code>item</code>
     * @param node parent node
     * @param childItem childs node item to be added.
     * @return children
     */
    public DfsStackTreeNode<E> addChild(DfsStackTreeNode<E> node, E childItem) {
        return node.addChild(childItem, threadNumber);
    }
}
