package cc.chungkwong.mathocr.extractor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Graph
 *
 * @param <V> type of vertex
 * @param <E> type of edge
 * @author Chan Chung Kwong
 */
public class Graph<V, E> {
    private final Map<E, Pair<V, V>> ends = new HashMap<>();
    private final Map<V, Set<E>> edges = new HashMap<>();
    private final Set<V> vertexs = new HashSet<>();

    /**
     * Create a empty graph
     */
    public Graph() {
    }

    /**
     * Added a edge
     *
     * @param edge  the edge
     * @param start the start vertex
     * @param end   the end vertex
     */
    public void add(E edge, V start, V end) {
        ends.put(edge, new Pair<>(start, end));
        add(start, edge);
        add(end, edge);
    }

    private void add(V start, E edge) {
        if (!edges.containsKey(start)) {
            edges.put(start, new HashSet<E>());
        }
        if (!vertexs.contains(start)) {
            vertexs.add(start);
        }
        edges.get(start).add(edge);
    }

    /**
     * Remove a edge
     *
     * @param edge to be removed
     */
    public void remove(E edge) {
        Pair<V, V> pair = ends.remove(edge);
        edges.get(pair.getKey()).remove(edge);
        edges.get(pair.getValue()).remove(edge);
    }

    /**
     * Get a edge that link two given vertexes
     *
     * @param start a vertex
     * @param end   another vertex
     * @return the edge or null
     */
    public E get(V start, V end) {
        if (edges.get(end).size() < edges.get(start).size()) {
            V tmp = end;
            end = start;
            start = tmp;
        }
        for (E e : edges.get(start)) {
            Pair<V, V> pair = ends.get(e);
            if ((pair.getKey() == start && pair.getValue() == end) || (pair.getValue() == start && pair.getKey() == end)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Merge two edge into one
     *
     * @param replacement new edge
     * @param replace0    to be replaced
     * @param replace1    to be replaced
     * @param vertex      the vertex between two edges
     */
    public void merge(E replacement, E replace0, E replace1, V vertex) {
        edges.get(vertex).remove(replace0);
        edges.get(vertex).remove(replace1);
        Pair<V, V> tmp = ends.remove(replace0);
        V start = tmp.getKey() == vertex ? tmp.getValue() : tmp.getKey();
        tmp = ends.remove(replace1);
        V end = tmp.getKey() == vertex ? tmp.getValue() : tmp.getKey();
        edges.get(start).remove(replace0);
        edges.get(end).remove(replace1);
        edges.get(start).add(replacement);
        edges.get(end).add(replacement);
        ends.put(replacement, new Pair<>(start, end));
    }

    /**
     * @return all vertexes
     */
    public Set<V> getVertexs() {
        return vertexs;
    }

    /**
     * @return all edges
     */
    public Set<E> getEdges() {
        return ends.keySet();
    }

    /**
     * @param vertex
     * @return all vertexes adjoint to a given vertex
     */
    public Set<E> getEdges(V vertex) {
        return edges.get(vertex);
    }

    /**
     * @param edge
     * @return the start vertex of a edge
     */
    public V getStart(E edge) {
        return ends.get(edge).getKey();
    }

    /**
     * @param edge
     * @return the end vertex of a edge
     */
    public V getEnd(E edge) {
        return ends.get(edge).getValue();
    }

    /**
     * @return the connected components
     */
    public Iterator<Graph<V, E>> getComponents() {
        return new Iterator<Graph<V, E>>() {
            private final HashSet<V> unvisited = new HashSet<>(vertexs);

            @Override
            public boolean hasNext() {
                return !unvisited.isEmpty();
            }

            @Override
            public Graph<V, E> next() {
                Graph<V, E> component = new Graph<>();
                LinkedList<V> found = new LinkedList<>();
                V joint = unvisited.iterator().next();
                found.push(joint);
                component.getVertexs().add(joint);
                while (!found.isEmpty()) {
                    joint = found.pop();
                    for (E edge : getEdges(joint)) {
                        V start = getStart(edge);
                        V end = getEnd(edge);
                        if (!component.getVertexs().contains(start)) {
                            found.push(start);
                        }
                        if (!component.getVertexs().contains(end)) {
                            found.push(end);
                        }
                        component.add(edge, start, end);
                    }
                }
                unvisited.removeAll(component.getVertexs());
                return component;
            }
        };
    }

    @Override
    public Graph<V, E> clone() {
        Graph<V, E> spare = new Graph<>();
        spare.vertexs.addAll(vertexs);
        spare.ends.putAll(ends);
        for (Map.Entry<V, Set<E>> entry : edges.entrySet()) {
            V key = entry.getKey();
            Set<E> value = entry.getValue();
            spare.edges.put(key, new HashSet<>(value));
        }
        return spare;
    }
}