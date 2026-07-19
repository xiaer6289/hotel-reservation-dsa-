//
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

