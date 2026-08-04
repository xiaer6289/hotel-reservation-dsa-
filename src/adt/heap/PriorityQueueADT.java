package adt.heap;

/**
 *
 * @author Low Enn Toong
 */
public interface PriorityQueueADT<T> {
    void enqueue(T data);
    T dequeue();
    T peek();
    boolean isEmpty();
    int size();
    void clear();
    PriorityQueueADT<T> copy();
}