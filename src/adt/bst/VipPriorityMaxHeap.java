package adt.bst;

import entity.VipGuest;

/**
 *
 * @author Low Enn Toong
 */
public class VipPriorityMaxHeap implements PriorityHeapInterface<VipGuest> {
    private VipGuest[] heap;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 10;

    public VipPriorityMaxHeap() {
        this(DEFAULT_CAPACITY);
    }

    public VipPriorityMaxHeap(int capacity) {
        if (capacity < 1) {
            capacity = DEFAULT_CAPACITY;
        }
        heap = new VipGuest[capacity];
        numberOfEntries = 0;
    }

    @Override
    public boolean add(VipGuest newEntry) {
        if (newEntry == null) {
            return false;
        }
        if (numberOfEntries == heap.length) {
            expandCapacity();
        }
        heap[numberOfEntries] = newEntry;
        reheapUp(numberOfEntries);
        numberOfEntries++;
        return true;
    }

    @Override
    public VipGuest removeHighestPriority() {
        if (isEmpty()) {
            return null;
        }
        VipGuest highest = heap[0];
        numberOfEntries--;
        heap[0] = heap[numberOfEntries];
        heap[numberOfEntries] = null;
        if (!isEmpty()) {
            reheapDown(0);
        }
        return highest;
    }

    @Override
    public VipGuest getHighestPriority() {
        return isEmpty() ? null : heap[0];
    }

    @Override
    public VipGuest getEntry(int index) {
        if (index < 0 || index >= numberOfEntries) {
            return null;
        }
        return heap[index];
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < numberOfEntries; i++) {
            heap[i] = null;
        }
        numberOfEntries = 0;
    }

    private void reheapUp(int childIndex) {
        while (childIndex > 0) {
            int parentIndex = (childIndex - 1) / 2;
            if (hasHigherPriority(heap[childIndex], heap[parentIndex])) {
                swap(childIndex, parentIndex);
                childIndex = parentIndex;
            } else {
                break;
            }
        }
    }

    private void reheapDown(int parentIndex) {
        while (true) {
            int leftChildIndex = parentIndex * 2 + 1;
            int rightChildIndex = parentIndex * 2 + 2;
            if (leftChildIndex >= numberOfEntries) {
                break;
            }
            int higherChildIndex = leftChildIndex;
            if (rightChildIndex < numberOfEntries && hasHigherPriority(heap[rightChildIndex], heap[leftChildIndex])) {
                higherChildIndex = rightChildIndex;
            }
            if (hasHigherPriority(heap[higherChildIndex], heap[parentIndex])) {
                swap(higherChildIndex, parentIndex);
                parentIndex = higherChildIndex;
            } else {
                break;
            }
        }
    }

    private boolean hasHigherPriority(VipGuest first, VipGuest second) {
        if (first.getPriority() != second.getPriority()) {
            return first.getPriority() > second.getPriority();
        }
        // Same tier: earlier request receives higher priority.
        return first.getRequestTime().isBefore(second.getRequestTime());
    }

    private void swap(int firstIndex, int secondIndex) {
        VipGuest temporary = heap[firstIndex];
        heap[firstIndex] = heap[secondIndex];
        heap[secondIndex] = temporary;
    }

    private void expandCapacity() {
        VipGuest[] largerHeap = new VipGuest[heap.length * 2];
        for (int i = 0; i < heap.length; i++) {
            largerHeap[i] = heap[i];
        }
        heap = largerHeap;
    }
}