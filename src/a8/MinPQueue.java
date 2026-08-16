package a8;

import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * A min priority queue of distinct elements of type `KeyType` associated with (extrinsic) double
 * priorities. Supports updating the priorities of elements currently in the queue, and guarantees
 * O(log N) performance for all modifying operations, where N is the queue size.
 */
public class MinPQueue<KeyType> {

    /**
     * Pairs an element `key` with its associated priority `priority`.
     */
    private record Entry<KeyType>(KeyType key, double priority) { }

    /**
     * ArrayList representing a binary min-heap of element-priority pairs.  Satisfies
     * `heap.get(i).priority() >= heap.get((i-1)/2).priority()` for all `i` in `[1..heap.size())`.
     */
    private final ArrayList<Entry<KeyType>> heap;

    /**
     * Associates each element in the queue with its index in `heap`.  Satisfies
     * `heap.get(index.get(e)).key().equals(e)` if `e` is an element in the queue.
     * Only maps elements that are in the queue (`index.size() == heap.size()`).
     */
    private final PacMap<KeyType, Integer> index;


    /**
     * Create an empty queue.
     */
    public MinPQueue() {
        index = new ProbingPacMap<>();
        heap = new ArrayList<>();
    }

    /**
     * Return whether this queue contains no elements.
     */
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Return the number of elements contained in this queue.
     */
    public int size() {
        return heap.size();
    }

    /**
     * Return an element associated with the smallest priority in this queue.  This is the same
     * element that would be removed by a call to `remove()` (assuming no mutations in between).
     * Throws a `NoSuchElementException` if this queue is empty.
     */
    public KeyType peek() {
        // Propagate exception from `List::getFirst()` if empty.
        return heap.getFirst().key();
    }

    /**
     * Return the minimum priority associated with an element in this queue.  Throws
     * a `NoSuchElementException` if this queue is empty.
     */
    public double minPriority() {
        return heap.getFirst().priority();
    }

    /**
     * Swap the `Entry`s at indices `i` and `j` in `heap`, updating `index` accordingly.  Requires
     * `0 <= i,j < heap.size()`.
     */
    private void swap(int i, int j) {
        Entry<KeyType> entryI = heap.get(i);
        Entry<KeyType> entryJ = heap.get(j);

        heap.set(i, entryJ);
        heap.set(j, entryI);
        index.put(entryI.key(), j);
        index.put(entryJ.key(), i);
    }

    // Helpers

    /**
     * Bubbles the entry at index `i` up toward the root until the heap property is restored.
     * Repeatedly swaps entry `i` with its parent while its priority is less than its parent's
     * priority. Requires `0 <= i < heap.size()`.
     */
    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap.get(i).priority() < heap.get(parent).priority()) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Bubbles the entry at index `i` down toward the leaves until the heap property is restored.
     * Repeatedly swaps entry `i` with its smallest-priority child while that child's priority is
     * less than entry `i`'s priority. Requires `0 <= i < heap.size()`.
     */
    private void bubbleDown(int i) {
        int size = heap.size();

        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < size && heap.get(left).priority() < heap.get(smallest).priority()) {
                smallest = left;
            }
            if (right < size && heap.get(right).priority() < heap.get(smallest).priority()) {
                smallest = right;
            }

            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }


    /**
     * Add element `key` to this queue, associated with priority `priority`.  Requires `key` is not
     * contained in this queue.
     */
    private void add(KeyType key, double priority) {
        int i = heap.size();
        heap.add(new Entry<>(key, priority));
        index.put(key, i);

        bubbleUp(i);

    }

    /**
     * Change the priority associated with element `key` to `priority`.  Requires that `key` is
     * contained in this queue.
     */
    private void update(KeyType key, double priority) {
        assert index.containsKey(key);

        int i = index.get(key);
        double oldPriority = heap.get(i).priority();
        heap.set(i, new Entry<>(key, priority));

        // changing priority of element 'key'
        if (priority < oldPriority) {
            bubbleUp(i);
        } else {
            bubbleDown(i);
        }
    }

    /**
     * If `key` is already contained in this queue, change its associated priority to `priority`.
     * Otherwise, add it to this queue with that priority.
     */
    public void addOrUpdate(KeyType key, double priority) {
        if (!index.containsKey(key)) {
            add(key, priority);
        } else {
            update(key, priority);
        }
    }

    /**
     * Remove and return the element associated with the smallest priority in this queue. If
     * multiple elements are tied for the smallest priority, an arbitrary element will be removed.
     * Throws NoSuchElementException if this queue is empty.
     */
    public KeyType remove() {
        if (isEmpty()) throw new NoSuchElementException();
        KeyType minKey = heap.getFirst().key();

        int last = heap.size() - 1;
        swap(0, last);
        heap.removeLast();
        index.remove(minKey);

        if (!isEmpty()) {
            bubbleDown(0);
        }

        return minKey;
    }

}
