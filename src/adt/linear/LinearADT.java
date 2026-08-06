package adt.linear;
/**
 * LinearADT is a generic interface defining the operations for a linear 
 * sequence of elements. It provides a standard contract for implementing 
 * list-like data structures (such as Singly/Doubly Linked Lists or ArrayLists)
 * with positional access, insertions, and deletions.
 *
 * @param <T> the type of elements held in this collection
 * @author team members
 */
public interface LinearADT<T> {
    void addFirst(T data);
    void addLast(T data);
    boolean addAt(int index, T data);
    T removeFirst();
    T removeLast();
    T removeAt(int index);
    T get(int index);
    boolean contains(T data);           // or search by key
    int indexOf(T data);
    boolean isEmpty();
    int size();
    void clear();
    void display();                     // helper for debugging/reports
}