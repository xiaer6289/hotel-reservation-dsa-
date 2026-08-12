package adt.heap;

/**
 * ADT interface for a priority queue implemented by a MaxHeap.
 *
 * @param <T> comparable entry stored in the heap
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