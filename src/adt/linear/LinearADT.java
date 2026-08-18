package adt.linear;

/**
 * Generic linear-sequence ADT contract.
 *
 * <p>The interface supports list operations, O(1) access to the first/last
 * element for queue-like workflows, and sequential traversal through a custom
 * visitor so clients do not depend on implementation nodes.</p>
 *
 * @param <T> type of elements stored in the sequence
 * @author Low Wei Shin
 */
public interface LinearADT<T> {

    void addFirst(T data);

    void addLast(T data);

    boolean addAt(int index, T data);

    T removeFirst();

    T removeLast();

    T removeAt(int index);

    T get(int index);

    /** Returns the first element without removing it, or null when empty. */
    T peekFirst();

    /** Returns the last element without removing it, or null when empty. */
    T peekLast();

    boolean contains(T data);

    int indexOf(T data);

    boolean isEmpty();

    int size();

    void clear();

    /** Visits each element from head to tail in O(n). */
    void traverse(LinearVisitor<T> visitor);

    /** Helper used by the existing console UI to display the sequence. */
    void display();
}