/*
 * DfsStackTreeNode.java, 28.05.2008
 */
package ru.ifmo.util.concurrent;

import ru.ifmo.util.CollectionUtils;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collection;

public class DfsStackTreeNode<E> {

    private E item;
    private ConcurrentMap<E, DfsStackTreeNode<E>> children;

    private DfsStackTreeNode<E> parent;

    public final AtomicBoolean wasLeft = new AtomicBoolean(false);

    /**
     * Create new root stack node (with parent == <code>null</code>)
     * @param x item
     * @param threadNumber number of threads
     */
    public DfsStackTreeNode(E x, int threadNumber) {
        this(x, null, threadNumber);
    }

    /**
     * Create child node for <code>parent</code> node.
     * @param item child node item
     * @param parent parent node
     * @param threadNumber number of threads
     */
    protected DfsStackTreeNode(E item, DfsStackTreeNode<E> parent, int threadNumber) {
        this.item = item;
        this.parent = parent;
        children = new ConcurrentHashMap<E, DfsStackTreeNode<E>>(
                CollectionUtils.defaultInitialCapacity(threadNumber),
                CollectionUtils.DEFAULT_LOAD_FACTOR, threadNumber);
    }

    /**
     * Add children with item <code>item</code>
     * @param item item
     * @param threadNumber number of threads
     * @return children
     */
    public DfsStackTreeNode<E> addChild(E item, int threadNumber) {
        DfsStackTreeNode<E> child = new DfsStackTreeNode<E>(item, this, threadNumber);
        DfsStackTreeNode<E> res = children.putIfAbsent(item, child);
        return (res == null) ? child : res;
    }

    public E getItem() {
        return item;
    }

    public DfsStackTreeNode<E> getParent() {
        return parent;
    }

    public Collection<DfsStackTreeNode<E>> getChildren() {
        return children.values();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Remove node from parents set of children. Node should be removed after it has been left.
     */
    public void remove() {
        assert wasLeft.get();
        
        if (parent != null) {
            parent.children.remove(item);
        }
    }
}
