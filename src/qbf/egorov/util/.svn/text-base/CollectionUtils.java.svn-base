/**
 * CollectionUtils.java, 16.05.2008
 */
package ru.ifmo.util;

import java.util.*;
import java.io.Serializable;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class CollectionUtils {
    /**
     * The default load factor for map.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public static final Deque EMPTY_DEQUE= new EmptyDeque();

    public static <T> Deque<T> emptyDeque() {
	    return (Deque<T>) EMPTY_DEQUE;
    }

    public static int defaultInitialCapacity(int entryNumber) {
        return (entryNumber * 4) / 3 + 1;
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
            return new StupidIterator<Object>();
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
    }
}
