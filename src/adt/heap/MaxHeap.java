package adt.heap;

/**
 *
 * @author Low Enn Toong
 */

public class MaxHeap<T extends Comparable<T>> implements PriorityQueueADT<T> {
    private T[] heap;
    private int size;
    private static final int DEFAULT_CAPACITY = 20;

    @SuppressWarnings("unchecked")
    public MaxHeap() {
        heap = (T[]) new Comparable[DEFAULT_CAPACITY + 1];
        size = 0;
    }

    @Override
    public void enqueue(T data) {
        if (data == null) {
            throw new IllegalArgumentException("Heap entry cannot be null.");
        }
        if (size == heap.length - 1) {
            expandCapacity();
        }
        size++;
        heap[size] = data;
        // Automatically reorganise the heap.
        reheapUp(size);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) return null;
        T highest = heap[1];
        heap[1] = heap[size];
        heap[size] = null;
        size--;
        if (size > 0) {
            reheapDown(1);
        }
        return highest;
    }

    @Override
    public T peek() {
        if (isEmpty()) return null;
        return heap[1];
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
        for (int i = 1; i <= size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PriorityQueueADT<T> copy() {
        MaxHeap<T> copiedHeap = new MaxHeap<>();
        copiedHeap.heap = (T[]) new Comparable[this.heap.length];
        System.arraycopy(this.heap, 1, copiedHeap.heap, 1, this.size);
        copiedHeap.size = this.size;
        return copiedHeap;
    }

    // ===== Helper methods =====
    private void reheapUp(int index) {
        while (index > 1) {
            int parent = index / 2;
            if (heap[index].compareTo(heap[parent]) > 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    private void reheapDown(int index) {
        while (index * 2 <= size) {
            int left = index * 2;
            int right = left + 1;
            int largerChild = left;
            if (right <= size && heap[right].compareTo(heap[left]) > 0) {
                largerChild = right;
            }
            if (heap[largerChild].compareTo(heap[index]) > 0) {
                swap(index, largerChild);
                index = largerChild;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        T temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    @SuppressWarnings("unchecked")
    private void expandCapacity() {
        T[] newHeap = (T[]) new Comparable[heap.length * 2];
        System.arraycopy(heap, 1, newHeap, 1, size);
        heap = newHeap;
    }
}