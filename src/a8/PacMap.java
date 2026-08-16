package a8;

/**
 * A map with keys of type 'K' and values of type `V`. This is a simpler, specialized
 * alternative to Java's `Map` interface. Supports iterating over keys in an unspecified
 * order.
 */
public interface PacMap<K, V> extends Iterable<K> {

    /**
     * Returns the number of keys currently associated with values in this map.
     */
    int size();

    /**
     * Returns whether a value is associated with the given `key`.
     */
    boolean containsKey(K key);

    /**
     * Returns the value associated with the given `key`. Throws a `NoSuchElementException` if no
     * value is associated with that key. Requires `key` is not null.
     */
    V get(K key);

    /**
     * Associates the given `value` to the given `key`. Requires `key` and `value` are not null.
     */
    void put(K key, V value);

    /**
     * Removes and returns the value associated with the given `key`. Throws a
     * `NoSuchElementException` if no value is associated with that key. Requires `key` is not null.
     */
    V remove(K key);
}