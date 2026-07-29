package adt.bst;

/**
 *
 * @author Lai Jen Feng
 */
public interface QueueInterface<T> {

    void enqueue(T newEntry);

    T dequeue();

    T getFront();

    boolean isEmpty();

    int getNumberOfEntries();

    void clear();
}