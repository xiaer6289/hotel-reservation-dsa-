package adt.linear;

/**
 * Generic node-based Doubly Linked List implementation of LinearADT.
 *
 * <p>Both head and tail are maintained, giving O(1) insertion/removal at both
 * ends. Indexed operations use bidirectional traversal: an index in the first
 * half starts from head, while an index in the second half starts from tail.
 * This uses the defining advantage of a doubly linked list instead of always
 * scanning from the front.</p>
 *
 * @param <T> type of elements stored in the list
 * @author team members
 */
public class DoublyLinkedList<T> implements LinearADT<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /** Internal list node; never exposed to client modules. */
    private static class Node<T> {
        private T data;
        private Node<T> prev;
        private Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    public DoublyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);

        if (isEmpty()) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }

        size++;
    }

    @Override
    public void addLast(T data) {
        Node<T> newNode = new Node<>(data);

        if (isEmpty()) {
            head = tail = newNode;
        } else {
            newNode.prev = tail;
            tail.next = newNode;
            tail = newNode;
        }

        size++;
    }

    @Override
    public boolean addAt(int index, T data) {
        if (index < 0 || index > size) {
            return false;
        }

        if (index == 0) {
            addFirst(data);
            return true;
        }

        if (index == size) {
            addLast(data);
            return true;
        }

        Node<T> nextNode = nodeAt(index);
        Node<T> previousNode = nextNode.prev;
        Node<T> newNode = new Node<>(data);

        newNode.prev = previousNode;
        newNode.next = nextNode;
        previousNode.next = newNode;
        nextNode.prev = newNode;
        size++;
        return true;
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }

        T data = head.data;

        if (size == 1) {
            head = tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }

        size--;
        return data;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }

        T data = tail.data;

        if (size == 1) {
            head = tail = null;
        } else {
            tail = tail.prev;
            tail.next = null;
        }

        size--;
        return data;
    }

    @Override
    public T removeAt(int index) {
        if (!isValidElementIndex(index)) {
            return null;
        }

        if (index == 0) {
            return removeFirst();
        }

        if (index == size - 1) {
            return removeLast();
        }

        Node<T> current = nodeAt(index);
        current.prev.next = current.next;
        current.next.prev = current.prev;
        size--;
        return current.data;
    }

    @Override
    public T get(int index) {
        if (!isValidElementIndex(index)) {
            return null;
        }
        return nodeAt(index).data;
    }

    @Override
    public T peekFirst() {
        return head == null ? null : head.data;
    }

    @Override
    public T peekLast() {
        return tail == null ? null : tail.data;
    }

    @Override
    public boolean contains(T data) {
        return indexOf(data) >= 0;
    }

    @Override
    public int indexOf(T data) {
        Node<T> current = head;
        int index = 0;

        while (current != null) {
            if (sameData(current.data, data)) {
                return index;
            }
            current = current.next;
            index++;
        }

        return -1;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        /*
         * Break links explicitly. This is not required for garbage collection,
         * but it releases node references immediately and leaves no accidental
         * chain reachable from the ADT.
         */
        Node<T> current = head;
        while (current != null) {
            Node<T> next = current.next;
            current.prev = null;
            current.next = null;
            current = next;
        }

        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public void traverse(LinearVisitor<T> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("LinearADT visitor cannot be null.");
        }

        Node<T> current = head;
        while (current != null) {
            visitor.visit(current.data);
            current = current.next;
        }
    }

    @Override
    public void display() {
        if (isEmpty()) {
            System.out.println("The list is empty.");
            return;
        }

        traverse(System.out::println);
    }

    /**
     * Returns the node at index using the shorter direction of travel.
     * Complexity is O(min(index, size - 1 - index)).
     */
    private Node<T> nodeAt(int index) {
        if (index < size / 2) {
            Node<T> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            return current;
        }

        Node<T> current = tail;
        for (int i = size - 1; i > index; i--) {
            current = current.prev;
        }
        return current;
    }

    private boolean isValidElementIndex(int index) {
        return index >= 0 && index < size;
    }

    private boolean sameData(T first, T second) {
        return first == second || (first != null && first.equals(second));
    }
}