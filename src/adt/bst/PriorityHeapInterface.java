package adt.bst;

/**
 *
 * @author Low Enn Toong
 */
public interface PriorityHeapInterface<T> {
    boolean add(T newEntry);
    T removeHighestPriority();
    T getHighestPriority();
    T getEntry(int index);
    int getNumberOfEntries();
    boolean isEmpty();
    void clear();
}