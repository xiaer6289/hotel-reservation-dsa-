package adt.heap;

import java.util.Comparator;

/**
 * Array-based generic MaxHeap implementation of PriorityQueueADT.
 *
 * <p>The heap uses 1-based indexing:</p>
 * <pre>
 * parent = index / 2
 * left   = index * 2
 * right  = index * 2 + 1
 * </pre>
 *
 * <p>A Comparator may be supplied for business-specific priority rules. If no
 * Comparator is supplied, entries must implement Comparable. This design lets
 * the VIP module keep loyalty-tier priority rules outside the registration
 * entity while the ADT remains reusable for other data types.</p>
 *
 * @param <T> type of entry stored in the heap
 * @author Low Enn Toong
 */
public class MaxHeap<T> implements PriorityQueueADT<T> {
    private static final int DEFAULT_CAPACITY = 20;
    private T[] heap;
    private int size;
    private final Comparator<? super T> comparator;

    public MaxHeap() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public MaxHeap(Comparator<? super T> comparator) {
        heap = (T[]) new Object[DEFAULT_CAPACITY + 1];
        size = 0;
        this.comparator = comparator;
    }

    @Override
    public void enqueue(T data) {
        requireData(data);

        if (size == heap.length - 1) {
            expandCapacity();
        }

        heap[++size] = data;
        reheapUp(size);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }

        T highestPriority = heap[1];
        removeAtIndex(1);
        return highestPriority;
    }

    @Override
    public T peek() {
        return isEmpty() ? null : heap[1];
    }

    @Override
    public boolean remove(T data) {
        if (data == null) {
            return false;
        }

        for (int index = 1; index <= size; index++) {
            if (sameEntry(heap[index], data)) {
                removeAtIndex(index);
                return true;
            }
        }

        return false;
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
        for (int index = 1; index <= size; index++) {
            heap[index] = null;
        }
        size = 0;
    }

    /**
     * Copies the internal heap layout directly in O(n), rather than repeatedly
     * enqueueing O(n log n). The copy has a separate backing array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public PriorityQueueADT<T> copy() {
        MaxHeap<T> copiedHeap = new MaxHeap<>(comparator);
        copiedHeap.heap = (T[]) new Object[this.heap.length];
        System.arraycopy(this.heap, 1, copiedHeap.heap, 1, this.size);
        copiedHeap.size = this.size;
        return copiedHeap;
    }

    private void reheapUp(int index) {
        int current = index;

        while (current > 1) {
            int parent = current / 2;

            if (compare(heap[current], heap[parent]) <= 0) {
                break;
            }

            swap(current, parent);
            current = parent;
        }
    }

    private void reheapDown(int index) {
        int current = index;

        while (current * 2 <= size) {
            int leftChild = current * 2;
            int rightChild = leftChild + 1;
            int largerChild = leftChild;

            if (rightChild <= size && compare(heap[rightChild], heap[leftChild]) > 0) {
                largerChild = rightChild;
            }

            if (compare(heap[largerChild], heap[current]) <= 0) {
                break;
            }

            swap(current, largerChild);
            current = largerChild;
        }
    }

    /**
     * Removes an arbitrary heap slot in O(log n) after its index is known.
     * The replacement can violate the heap property upward or downward, so the
     * direction is selected from its relationship with the parent.
     */
    private void removeAtIndex(int index) {
        if (index < 1 || index > size) {
            return;
        }

        heap[index] = heap[size];
        heap[size] = null;
        size--;

        if (index > size) {
            return; // Removed the last entry; no reheap is required.
        }

        if (index > 1 && compare(heap[index], heap[index / 2]) > 0) {
            reheapUp(index);
        } else {
            reheapDown(index);
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(T first, T second) {
        if (comparator != null) {
            return comparator.compare(first, second);
        }

        if (first instanceof Comparable<?>) {
            return ((Comparable<? super T>) first).compareTo(second);
        }

        throw new IllegalStateException("A Comparator is required when heap entries do not implement Comparable.");
    }

    private boolean sameEntry(T first, T second) {
        return first == second || (first != null && first.equals(second));
    }

    private void swap(int firstIndex, int secondIndex) {
        T temp = heap[firstIndex];
        heap[firstIndex] = heap[secondIndex];
        heap[secondIndex] = temp;
    }

    @SuppressWarnings("unchecked")
    private void expandCapacity() {
        T[] expandedHeap = (T[]) new Object[heap.length * 2];
        System.arraycopy(heap, 1, expandedHeap, 1, size);
        heap = expandedHeap;
    }

    private void requireData(T data) {
        if (data == null) {
            throw new IllegalArgumentException("Heap entry cannot be null.");
        }
    }
}