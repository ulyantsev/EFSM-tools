/**
 * CollectionUtils.java, 16.05.2008
 */
package qbf.egorov.util;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.io.Serializable;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class CollectionUtils {
    @SuppressWarnings("rawtypes")
	public static final Deque EMPTY_DEQUE = new EmptyDeque();

    @SuppressWarnings("unchecked")
	public static <T> Deque<T> emptyDeque() {
	    return (Deque<T>) EMPTY_DEQUE;
    }

    private static class EmptyDeque extends AbstractList<Object>
            implements Deque<Object>, Serializable {

        public int size() {
            return 0;
        }

        public boolean contains(Object obj) {
            return false;
        }

        public Object get(int index) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        public Iterator<Object> descendingIterator() {
            return new StupidIterator<>();
        }

        public void addFirst(Object o) {
            throw new UnsupportedOperationException();
        }

        public void addLast(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean offerFirst(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean offerLast(Object o) {
            throw new UnsupportedOperationException();
        }

        public Object removeFirst() {
            throw new NoSuchElementException();
        }

        public Object removeLast() {
            throw new NoSuchElementException();
        }

        public Object pollFirst() {
            return null;
        }

        public Object pollLast() {
            return null;
        }

        public Object getFirst() {
            throw new NoSuchElementException();
        }

        public Object getLast() {
            throw new NoSuchElementException();
        }

        public Object peekFirst() {
            return null;
        }

        public Object peekLast() {
            return null;
        }

        public boolean removeFirstOccurrence(Object o) {
            return false;
        }

        public boolean removeLastOccurrence(Object o) {
            return false;
        }

        public boolean offer(Object o) {
            throw new UnsupportedOperationException();
        }

        public Object remove() {
            throw new NoSuchElementException();
        }

        public Object poll() {
            return null;
        }

        public Object element() {
            throw new NoSuchElementException();
        }

        public Object peek() {
            return null;
        }

        public void push(Object o) {
            throw new UnsupportedOperationException();
        }

        public Object pop() {
            throw new NoSuchElementException();
        }

		@Override
		public boolean removeIf(Predicate<? super Object> filter) {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public Spliterator<Object> spliterator() {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public Stream<Object> stream() {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public Stream<Object> parallelStream() {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public void forEach(Consumer<? super Object> action) {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public void replaceAll(UnaryOperator<Object> operator) {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}

		@Override
		public void sort(Comparator<? super Object> c) {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}
    }

    private static class StupidIterator<E> implements Iterator<E> {
        public boolean hasNext() {
            return false;
        }

        public E next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			throw new AssertionError();
		}
    }
}
