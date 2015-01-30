/**
 * ConcurrentHashSet.java, 16.05.2008
 */
package qbf.egorov.util.concurrent;

import qbf.egorov.util.CollectionUtils;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

	@Override
	public Spliterator<E> spliterator() {
		// TODO Auto-generated method stub
		throw new AssertionError();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		// TODO Auto-generated method stub
		throw new AssertionError();
	}

	@Override
	public Stream<E> stream() {
		// TODO Auto-generated method stub
		throw new AssertionError();
	}

	@Override
	public Stream<E> parallelStream() {
		// TODO Auto-generated method stub
		throw new AssertionError();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		// TODO Auto-generated method stub
		throw new AssertionError();
	}
}
