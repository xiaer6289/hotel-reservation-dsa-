package adt.bst;

/**
 *
 * @author Lai Jen Feng
 */
public class RegistrationQueue<T> implements QueueInterface<T> {

    private Node frontNode;
    private Node rearNode;
    private int numberOfEntries;

    public RegistrationQueue() {
        frontNode = null;
        rearNode = null;
        numberOfEntries = 0;
    }

    @Override
    public void enqueue(T newEntry) {
        Node newNode = new Node(newEntry);

        if (frontNode == null) {
            frontNode = newNode;
            rearNode = newNode;
        } else {
            rearNode.next = newNode;
            rearNode = newNode;
        }

        numberOfEntries++;
    }

    @Override
    public T dequeue() {
        if (frontNode == null) {
            return null;
        }

        T frontData = frontNode.data;
        frontNode = frontNode.next;
        numberOfEntries--;

        if (frontNode == null) {
            rearNode = null;
        }

        return frontData;
    }

    @Override
    public T getFront() {
        if (frontNode == null) {
            return null;
        }

        return frontNode.data;
    }

    @Override
    public boolean isEmpty() {
        return frontNode == null;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public void clear() {
        frontNode = null;
        rearNode = null;
        numberOfEntries = 0;
    }

    private class Node {

        private T data;
        private Node next;

        private Node(T data) {
            this.data = data;
            this.next = null;
        }
    }
}