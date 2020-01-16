package cc.chungkwong.mathocr.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skeleton tracer that use thinning
 *
 * @author Chan Chung Kwong
 */
public class ThinTracer {
    //	public static TraceList trace(Graph<Junction,Segment> graph){
//
    private static final double DOT_THREHOLD = 0.5;

    public static Graph<Junction, Segment> trace(Raster image) {
        Graph<Junction, Segment> graph = buildRawGraph(image);
        simplifyGraph(graph);
        return graph;
    }

    public static Graph<Junction, Segment> buildRawGraph(Raster image) {
        StrokeWidthTransform.StrokeSpace strokeSpace = StrokeWidthTransform.transform(image);
        int length = strokeSpace.getThicknessH().length;
        int[] thicknessSq = new int[length];
        for (int i = 0; i < length; i++) {
            thicknessSq[i] = Math.min(square(strokeSpace.getThicknessH()[i]), 2 * square(strokeSpace.getThicknessS()[i]));
        }
        Thinning.thin(image);//FIXME Changed input
        return buildRawGraph(image, thicknessSq);
    }

    private static Graph<Junction, Segment> buildRawGraph(Raster Raster, int[] thicknessSq) {
        int width = Raster.getWidth();
        int height = Raster.getHeight();
        List<Segment> segments = new ArrayList<>();
        Map<Junction, Set<Segment>> vertexs = new HashMap<>();
        Component[] index = new Component[width * height];
        followEdge(index, segments, Raster, thicknessSq);
        followJoint(index, vertexs, Raster, thicknessSq);
        return buildRawGraph(segments, vertexs);
    }

    private static void followEdge(Component[] index, List<Segment> edges, Raster Raster, int[] thicknessSq) {
        int width = Raster.getWidth();
        byte[] bits = Raster.getData();
        for (int found = 0; found < bits.length; found++) {
            if (bits[found] != 0 || index[found] != null) {
                continue;
            }
            if (isEdge(found, Raster)) {
                Segment tracing = new Segment();
                List<TracePoint> points = tracing.getTrace().getPoints();
                points.add(TracePoint.fromIndex(found, width));
                index[found] = tracing;
                int[] init = getNeighbors(found, Raster);
                int last = found, curr = init[0];
                while (index[curr] == null && isEdge(curr, Raster)) {
                    points.add(TracePoint.fromIndex(curr, width));
                    index[curr] = tracing;
                    int[] cand = getNeighbors(curr, Raster);
                    if (cand[1] == -1) {
                        break;
                    }
                    int tmp = curr;
                    curr = last != cand[0] ? cand[0] : cand[1];
                    last = tmp;
                }
                if (init[1] != -1) {
                    last = found;
                    curr = init[1];
                    while (index[curr] == null && isEdge(curr, Raster)) {
                        points.add(0, TracePoint.fromIndex(curr, width));
                        index[curr] = tracing;
                        int[] cand = getNeighbors(curr, Raster);
                        if (cand[1] == -1) {
                            break;
                        }
                        int tmp = curr;
                        curr = last != cand[0] ? cand[0] : cand[1];
                        last = tmp;
                    }
                }
                //tracing.setThick(points.stream().mapToInt((p)->thicknessSq[p.toIndex(width)]).sorted().skip(points.size()/2).findFirst().getAsInt());
                int thickSum = 0;
                for (TracePoint p : points) {
                    thickSum += thicknessSq[p.toIndex(width)];
                }
                tracing.setThick(thickSum / points.size() + 1);
                tracing.updateAngles();
                edges.add(tracing);
            }
        }
    }

    private static boolean isEdge(int ind, Raster Raster) {
        int width = Raster.getWidth();
        byte[] pixels = Raster.getData();
        boolean[] neighbor = {
                pixels[ind - width] == 0,
                pixels[ind - width + 1] == 0,
                pixels[ind + 1] == 0,
                pixels[ind + width + 1] == 0,
                pixels[ind + width] == 0,
                pixels[ind + width - 1] == 0,
                pixels[ind - 1] == 0,
                pixels[ind - width - 1] == 0
        };
        boolean last = neighbor[7];
        int components = 0;
        int points = 0;
        for (int i = 0; i < 8; i++) {
            if (neighbor[i]) {
                ++points;
                if (!last) {
                    ++components;
                }
            }
            last = neighbor[i];
        }
        return components == 2 && points == 2;
    }

    private static int[] getNeighbors(int ind, Raster Raster) {
        int width = Raster.getWidth();
        byte[] pixels = Raster.getData();
        int[] neighbor = {
                ind + 1, ind + width, ind + width - 1, ind + width + 1, ind - width, ind - width + 1, ind - 1, ind - width - 1
        };
        int[] neighbors = {-1, -1};
        int k = 0;
        for (int i = 0; i < 8; i++) {
            if (pixels[neighbor[i]] == 0) {
                neighbors[k++] = neighbor[i];
            }
        }
        return neighbors;
    }

    private static void followJoint(Component[] index, Map<Junction, Set<Segment>> neighborhood, Raster Raster, int[] thicknessSq) {
        int width = Raster.getWidth();
        int[] offsets = {1, -width + 1, -width, -width - 1, -1, width - 1, width, width + 1};
        byte[] bits = Raster.getData();
        for (int found = 0; found < bits.length; found++) {
            if (bits[found] != 0 || index[found] != null) {
                continue;
            }
            Junction tracing = new Junction(new Trace(new LinkedList<TracePoint>()));
            HashSet<Segment> neighbors = new HashSet<>();
            int thick = thicknessSq[found];
            neighborhood.put(tracing, neighbors);
            List<TracePoint> points = tracing.getTrace().getPoints();
            points.add(TracePoint.fromIndex(found, width));
            index[found] = tracing;
            LinkedList<Integer> toTrace = new LinkedList<>();
            toTrace.push(found);
            while (!toTrace.isEmpty()) {
                Integer pop = toTrace.pop();
                for (int offset : offsets) {
                    int curr = pop + offset;
                    if (bits[curr] == 0) {
                        if (index[curr] == null) {
                            points.add(TracePoint.fromIndex(curr, width));
                            index[curr] = tracing;
                            toTrace.push(curr);
                            if (thicknessSq[curr] > thick) {
                                thick = thicknessSq[curr];
                            }
                        } else if (index[curr] instanceof Segment) {
                            neighbors.add((Segment) index[curr]);
                        }
                    }
                }
            }
            tracing.setThick(thick);
        }
    }

    private static Graph<Junction, Segment> buildRawGraph(List<Segment> segments, Map<Junction, Set<Segment>> vertexs) {
        Graph<Junction, Segment> graph = new Graph<>();
        Map<Segment, List<Junction>> ends = new HashMap<>();
        for (Segment segment : segments) {
            ends.put(segment, new ArrayList<Junction>(2));
        }
        for (Map.Entry<Junction, Set<Segment>> entry : vertexs.entrySet()) {
            Junction vertex = entry.getKey();
            for (Segment substroke : entry.getValue()) {
                ends.get(substroke).add(vertex);
            }
            graph.getVertexs().add(vertex);
        }
        for (Map.Entry<Segment, List<Junction>> entry : ends.entrySet()) {
            Segment key = entry.getKey();
            List<Junction> value = entry.getValue();
            TracePoint segmentStart = key.getTrace().getStart();
            if (value.isEmpty()) {
                Junction joint = new Junction(new Trace(new LinkedList<TracePoint>()));
                joint.getTrace().getPoints().add(segmentStart);
                graph.add(key, joint, joint);
            } else if (value.size() == 1) {
                graph.add(key, value.get(0), value.get(0));
            } else {
                Junction jointStart;
                Junction jointEnd;
                if (value.size() != 2) {
                    throw new RuntimeException();
                }
                if (isNeighbor(value.get(0).getTrace().getPoints(), segmentStart)) {
                    jointStart = value.get(0);
                    jointEnd = value.get(1);
                } else {
                    jointStart = value.get(1);
                    jointEnd = value.get(0);
                }
                graph.add(key, jointStart, jointEnd);
            }
        }
        return graph;
    }

    private static boolean isNeighbor(List<TracePoint> p, TracePoint q) {
        for (TracePoint r : p) {
            if (isNeighbor(r, q))
                return true;
        }
        return false;
    }

    private static boolean isNeighbor(TracePoint p, TracePoint q) {
        int dx = p.getX() - q.getX();
        int dy = p.getY() - q.getY();
        return dx >= -1 && dx <= 1 && dy >= -1 && dy <= 1;
    }

    public static void simplifyGraph(Graph<Junction, Segment> graph) {
        double thick = getAverageThick(graph.getEdges());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Segment edge : graph.getEdges()) {
                int t = square(edge.getTrace().getPoints().size());
                if (t<=thick / 2){//t <= edge.getThick() || t <= thick || t <= 9) {
                    removeEdge(edge, graph);
                    changed = true;
                    break;
                }
            }
        }
        thick = getAverageThick(graph.getEdges());
        int minDotSize = (int) (thick / 4);
        for (Iterator<Junction> iterator = graph.getVertexs().iterator(); iterator.hasNext(); ) {
            Junction v = iterator.next();
            if (graph.getEdges(v) != null && graph.getEdges(v).isEmpty() && v.getThick() < minDotSize) {
                iterator.remove();
            }
        }
    }

    private static int getAverageThick(Collection<Segment> segments) {
        if (segments.isEmpty())
            return 0;
        int thickSum = 0;
        for (Segment s : segments) {
            thickSum += s.getThick();
        }
        return thickSum / segments.size() + 1;
    }

    private static void removeEdge(Segment edge, Graph<Junction, Segment> graph) {
        Junction joint1 = graph.getStart(edge);
        Junction joint2 = graph.getEnd(edge);
        graph.remove(edge);
        if (joint1 == joint2) {
            return;
        }
        joint1.getTrace().getPoints().addAll(joint2.getTrace().getPoints());
        joint1.getTrace().getPoints().addAll(edge.getTrace().getPoints());
        List<Segment> affected = new ArrayList<>(graph.getEdges(joint2));
        for (Segment substroke : affected) {
            Junction start = graph.getStart(substroke);
            Junction end = graph.getEnd(substroke);
            graph.remove(substroke);
            if (start == joint2) {
                start = joint1;
            }
            if (end == joint2) {
                end = joint1;
            }
            graph.add(substroke, start, end);
        }
        graph.getVertexs().remove(joint2);
    }

    private static int square(int i) {
        return i * i;
    }
}