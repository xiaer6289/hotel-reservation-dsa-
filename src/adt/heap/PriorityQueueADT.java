package adt.heap;

/**
 * Generic priority-queue ADT. The highest-priority entry is always available
 * at the front. A MaxHeap is used as the concrete implementation.
 *
 * @param <T> entry type stored in the priority queue
 * @author Low Enn Toong
 */
public interface PriorityQueueADT<T> {
    /** Inserts an entry and restores priority order. */
    void enqueue(T data);

    /** Removes and returns the highest-priority entry, or null when empty. */
    T dequeue();

    /** Returns the highest-priority entry without removing it. */
    T peek();

    /**
     * Removes the first matching entry from the heap and restores heap order.
     * This supports efficient cancellation without rebuilding the entire heap.
     */
    boolean remove(T data);

    /** Returns true when the priority queue has no entries. */
    boolean isEmpty();

    /** Returns the number of entries currently stored. */
    int size();

    /** Removes all entries. */
    void clear();
    
    /**
     * Creates an independent heap structure containing the same entry
     * references and priority rule. Mutating the copy's structure does not
     * change the original heap.
     */
    PriorityQueueADT<T> copy();
}