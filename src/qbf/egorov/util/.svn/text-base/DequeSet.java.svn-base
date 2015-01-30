/*
 * Developed by eVelopers Corporation - 21.05.2008
 */
package ru.ifmo.util;

import org.apache.commons.lang.NotImplementedException;

import java.util.*;

public class DequeSet<E> implements Deque<E> {
    private Deque<E> deque = new LinkedList<E>();
    private Map<E, Integer> elements = new HashMap<E, Integer>();

    public DequeSet() {
        elements = new HashMap<E, Integer>();
    }

    public DequeSet(int initialCapacity) {
       elements = new HashMap<E, Integer>(initialCapacity);
    }

    public void addFirst(E e) {
        deque.addFirst(e);
        addElement(e);
    }

    public void addLast(E e) {
        deque.addLast(e);
        addElement(e);
    }

    public boolean offerFirst(E e) {
        if (deque.offerFirst(e)) {
            addElement(e);
            return true;
        }
        return false;
    }

    public boolean offerLast(E e) {
        if (deque.offerLast(e)) {
            addElement(e);
            return true;
        }
        return false;
    }

    public E removeFirst() {
        E res = deque.removeFirst();
        removeElement(res);
        return res;
    }

    public E removeLast() {
        E res = deque.removeLast();
        removeElement(res);
        return res;
    }

    public E pollFirst() {
        E res = deque.pollFirst();
        removeElement(res);
        return res;
    }

    public E pollLast() {
        E res = deque.pollLast();
        removeElement(res);
        return res;
    }

    public E getFirst() {
        return deque.getFirst();
    }

    public E getLast() {
        return deque.getLast();
    }

    public E peekFirst() {
        return deque.peekFirst();
    }

    public E peekLast() {
        return deque.peekLast();
    }

    public boolean removeFirstOccurrence(Object o) {
        if (deque.removeFirstOccurrence(o)) {
            removeElement((E) o);
            return true;
        }
        return false;
    }

    public boolean removeLastOccurrence(Object o) {
        if (deque.removeLastOccurrence(o)) {
            removeElement((E) o);
            return true;
        }
        return false;
    }

    public boolean add(E e) {
        if (deque.add(e)) {
            addElement(e);
            return true;
        }
        return false;
    }

    public boolean offer(E e) {
        if (deque.offer(e)) {
            addElement(e);
            return true;
        }
        return false;
    }

    public E remove() {
        E res = deque.remove();
        removeElement(res);
        return res;
    }

    public E poll() {
        E res = deque.poll();
        removeElement(res);
        return res;
    }

    public E element() {
        return deque.element();
    }

    public E peek() {
        return deque.peek();
    }

    public void push(E e) {
        deque.push(e);
        addElement(e);
    }

    public E pop() {
        E res = deque.pop();
        removeElement(res);
        return res;
    }

    public boolean remove(Object o) {
        if (deque.remove(o)) {
            removeElement((E) o);
            return true;
        }
        return false;
    }

    public boolean contains(Object o) {
        return elements.containsKey(o);
    }

    public int size() {
        return deque.size();
    }

    public Iterator<E> iterator() {
        return deque.iterator();
    }

    public Iterator<E> descendingIterator() {
        return deque.descendingIterator();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public Object[] toArray() {
        return deque.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o: c) {
            if (!elements.containsKey(o)) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(Collection<? extends E> c) {
        for (E e: c) {
            add(e);
        }
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        for (Object e: c) {
            remove(e);
        }
        return true;
    }

    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException();    //TODO: implement me!
    }

    public void clear() {
        deque.clear();
        elements.clear();
    }

    public int hashCode() {
        return deque.hashCode();
    }

    private void addElement(E e) {
        Integer count = elements.get(e);
        elements.put(e, count == null ? 1 : count + 1);
    }

    private void removeElement(E e) {
        if (e != null)  {
            int count = elements.get(e);
            if (count == 1) {
                elements.remove(e);
            } else {
                elements.put(e, count - 1);
            }
        }
    }
}
