package cc.chungkwong.mathocr.extractor;

/**
 * Pair of objects
 *
 * @param <K> type of first object
 * @param <V> type of second object
 * @author Chan Chung Kwong
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Create a pair
     *
     * @param key   the first object
     * @param value the second object
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    private static boolean equals(Object a, Object b) {
        return a != null ? a.equals(b) : b == null;
    }

    private static int hashCode(Object value) {
        return value != null ? value.hashCode() : 0;
    }

    /**
     * @return the first object
     */
    public K getKey() {
        return key;
    }

    /**
     * @return the second object
     */
    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pair && equals(key, ((Pair) obj).key) && equals(value, ((Pair) obj).value);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + hashCode(key);
        hash = 97 * hash + hashCode(value);
        return hash;
    }

    @Override
    public String toString() {
        return "(" + key + "," + value + ")";
    }
}