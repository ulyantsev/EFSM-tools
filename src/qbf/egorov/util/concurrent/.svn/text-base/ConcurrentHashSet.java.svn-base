/**
 * ConcurrentHashSet.java, 16.05.2008
 */
package ru.ifmo.util.concurrent;

import ru.ifmo.util.CollectionUtils;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
    private ConcurrentHashMap<E, Object> map;

    public ConcurrentHashSet(int entryNumber, int threadNumber) {
        this(CollectionUtils.defaultInitialCapacity(entryNumber),
                CollectionUtils.DEFAULT_LOAD_FACTOR, threadNumber);
    }

    public ConcurrentHashSet(int initialCapacity, float loadFactor, int threadNumber) {
        map = new ConcurrentHashMap<E, Object>(initialCapacity, loadFactor, threadNumber);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public boolean add(E e) {
        return map.putIfAbsent(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o, Boolean.TRUE);
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        map.clear();
    }
}
