package cc.chungkwong.mathocr.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Greedy graph tracer
 *
 * @author Chan Chung Kwong
 */
public class GraphTracer {
    private static final double ANGLE_THREHOLD = Math.PI / 6;
    private static final double TWO_PI = 2 * Math.PI;

    public static TraceList trace(Graph<Junction, Segment> graph) {
        List<Trace> traces = new ArrayList<>();
        traceDot(graph, traces);
        for (Iterator<Graph<Junction, Segment>> iterator = graph.getComponents(); iterator.hasNext(); ) {
            Graph<Junction, Segment> component = iterator.next();
            Graph<Junction, Segment> componentBackup = component.clone();
            traceThrough(component);
            Map<Trace, Pair<Junction, Junction>> pretrace = traceBend(component);
            fixDouble(pretrace, componentBackup);
            List<Trace> list = new ArrayList<>(pretrace.keySet());
            Collections.sort(list, new Comparator<Trace>() {
                @Override
                public int compare(Trace t0, Trace t1) {
                    return t0.getPoints().size() - t1.getPoints().size();
                }
            });
            if (list.size() >= 2 && list.get(0).getPoints().size() <= list.get(1).getPoints().size() / 16) {
                traces.addAll(list.subList(1, list.size()));
            } else {
                traces.addAll(list);
            }
//			traces.addAll(pretrace.keySet());
        }
        return new TraceList(traces);
    }

    private static void traceDot(Graph<Junction, Segment> graph, List<Trace> traces) {
        for (Iterator<Junction> iterator = graph.getVertexs().iterator(); iterator.hasNext(); ) {
            Junction vertex = iterator.next();
            if (graph.getEdges(vertex) == null || graph.getEdges(vertex).isEmpty()) {
                int count = vertex.getTrace().getPoints().size();
                int sumx = 0;
                int sumy = 0;
                for (TracePoint p : vertex.getTrace().getPoints()) {
                    sumx += p.getX();
                    sumy += p.getY();
                }
                Trace trace = new Trace(new LinkedList<TracePoint>());
                trace.getPoints().add(new TracePoint(sumx / count, sumy / count));
                traces.add(trace);
                iterator.remove();
            }
        }
    }

    private static void traceThrough(Graph<Junction, Segment> graph) {
        class Ray {
            private final Segment segment;
            private final boolean forward;

            public Ray(Segment segment, boolean forward) {
                this.segment = segment;
                this.forward = forward;
            }

            public Segment getSegment() {
                return segment;
            }

            public boolean isForward() {
                return forward;
            }

            public Junction getStart(Graph<Junction, Segment> graph) {
                return forward ? graph.getStart(segment) : graph.getEnd(segment);
            }

            //			public Junction getEnd(Graph<Junction,Segment> graph){
//				return forward?graph.getEnd(segment):graph.getStart(segment);
//			}
            public double getStartAngle() {
                return forward ? segment.getAngleBegin() : segment.getAngleEnd() + Math.PI;
            }

            public double getEndAngle() {
                return forward ? segment.getAngleEnd() : segment.getAngleBegin() + Math.PI;
            }

            public Ray reverse() {
                return new Ray(segment, !forward);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Ray && segment == ((Ray) obj).segment && forward == ((Ray) obj).forward;
            }

            @Override
            public int hashCode() {
                int hash = 3;
                hash = 97 * hash + (segment != null ? segment.hashCode() : 0);
                hash = 97 * hash + (this.forward ? 1 : 0);
                return hash;
            }

            @Override
            public String toString() {
                return "(" + segment + "," + forward + ")";
            }
        }
        class Turn {
            private final Ray start, end;
            private final double angle;

            public Turn(Ray start, Ray end) {
                this.start = start;
                this.end = end;
                this.angle = Math.abs(normalize(end.getStartAngle() + Math.PI - start.getStartAngle()));
            }

            public Ray getStart() {
                return start;
            }

            public Ray getEnd() {
                return end;
            }

            public double getAngle() {
                return angle;
            }
        }
        HashMap<Ray, Ray> status = new HashMap<>();
        HashMap<Ray, Ray> ends = new HashMap<>();
        List<Turn> turns = new ArrayList<>();
        for (Junction joint : graph.getVertexs()) {
            List<Ray> rays = new ArrayList<>();
            for (Segment edge : graph.getEdges(joint)) {
                if (graph.getStart(edge) == joint) {
                    Ray ray = new Ray(edge, true);
                    rays.add(ray);
                    status.put(ray, ray);
                    ends.put(ray, ray.reverse());
                }
                if (graph.getEnd(edge) == joint) {
                    Ray ray = new Ray(edge, false);
                    rays.add(ray);
                    status.put(ray, ray);
                    ends.put(ray, ray.reverse());
                }
            }
            for (int i = 0; i < rays.size(); i++) {
                for (int j = i + 1; j < rays.size(); j++) {
                    if (rays.get(i).segment != rays.get(j).segment) {
                        turns.add(new Turn(rays.get(i), rays.get(j)));
                    }
                }
            }
        }
        Collections.sort(turns, new Comparator<Turn>() {
            @Override
            public int compare(Turn t0, Turn t1) {
                double tmp = -t0.getAngle() + t1.getAngle();
                return tmp < 0 ? -1 : (tmp > 0 ? 1 : 0);
            }
        });
        while (!turns.isEmpty()) {
            Turn turn = turns.remove(turns.size() - 1);
            Ray startRaw = turn.getStart();
            Ray endRaw = turn.getEnd();
            Ray start = status.get(startRaw);
            Ray end = status.get(endRaw);
            if (start == null || end == null || start.getSegment() == end.getSegment()) {
                continue;
            }
            Segment concat = new Segment(
                    new Trace(new ArrayList<TracePoint>(start.getSegment().getTrace().getPoints().size() + end.getSegment().getTrace().getPoints().size())),
                    Math.max(start.getSegment().getThick(), end.getSegment().getThick()),
                    start.getEndAngle() + Math.PI,
                    end.getEndAngle()
            );
            List<TracePoint> points = concat.getTrace().getPoints();
            points.addAll(start.getSegment().getTrace().getPoints());
            if (start.isForward()) {
                Collections.reverse(points);
            }
            points.addAll(end.getSegment().getTrace().getPoints());
            if (!end.isForward()) {
                Collections.reverse(points.subList(points.size() - end.getSegment().getTrace().getPoints().size(), points.size()));
            }
            status.remove(endRaw);
            status.remove(startRaw);
            Ray forward = new Ray(concat, true);
            Ray backward = new Ray(concat, false);
            ends.put(forward, ends.get(end));
            ends.put(backward, ends.get(start));
            status.put(ends.get(end), backward);
            status.put(ends.get(start), forward);
            graph.merge(concat, start.getSegment(), end.getSegment(), end.getStart(graph));
            //System.out.println(Graph.toString(graph));
        }
    }

    private static Map<Trace, Pair<Junction, Junction>> traceBend(Graph<Junction, Segment> graph) {
        Map<Trace, Pair<Junction, Junction>> traceEnds = new HashMap<>();
        for (Segment edge : graph.getEdges()) {
            traceEnds.put(edge.getTrace(), new Pair<>(graph.getStart(edge), graph.getEnd(edge)));
        }
        return traceEnds;
    }

    private static void fixDouble(Map<Trace, Pair<Junction, Junction>> traceEnds, final Graph<Junction, Segment> graph) {
        final Map<Junction, Integer> degree = new HashMap<>();
        for (Junction vertex : graph.getVertexs()) {
            int d = 0;
            for (Segment edge : graph.getEdges(vertex))
                d += graph.getStart(edge) == graph.getEnd(edge) ? 2 : 1;
            degree.put(vertex, d);
        }
        Map<Junction, Trace> jt = new HashMap<>();
        for (Map.Entry<Trace, Pair<Junction, Junction>> entry : traceEnds.entrySet()) {
            Trace key = entry.getKey();
            Pair<Junction, Junction> value = entry.getValue();
            if (jt.containsKey(value.getKey())) {
                jt.put(value.getKey(), null);
            } else {
                jt.put(value.getKey(), key);
            }
            if (jt.containsKey(value.getValue())) {
                jt.put(value.getValue(), null);
            } else {
                jt.put(value.getValue(), key);
            }
        }
        final Map<Segment, Double> turning = new HashMap<>();
        List<Segment> linkages = new ArrayList<>();
        for (Segment edge : graph.getEdges()) {
            Junction start = graph.getStart(edge);
            Junction end = graph.getEnd(edge);
            if (start == end || degree.get(start) % 2 == 0 || degree.get(end) % 2 == 0) {
                continue;
            }
            Trace startTrace = jt.get(start);
            Trace endTrace = jt.get(end);
            if (startTrace != null && endTrace != null && startTrace != endTrace) {
                turning.put(edge, getTurning(startTrace, edge, endTrace, traceEnds, graph));
                linkages.add(edge);
            }
        }
        ;
        Collections.sort(linkages, new Comparator<Segment>() {
            @Override
            public int compare(Segment t0, Segment t1) {
                int tmp0 = -degree.get(graph.getStart(t0)) - degree.get(graph.getEnd(t0));
                int tmp1 = -degree.get(graph.getStart(t1)) - degree.get(graph.getEnd(t1));
                if (tmp0 > tmp1)
                    return 1;
                else if (tmp0 < tmp1) {
                    return -1;
                } else {
                    double tmp = turning.get(t0) - turning.get(t1);
                    return tmp < 0 ? -1 : (tmp > 0 ? 1 : 0);
                }
            }
        });
        for (Segment edge : linkages) {
            Junction start = graph.getStart(edge);
            Junction end = graph.getEnd(edge);
            Trace startTrace = jt.get(start);
            Trace endTrace = jt.get(end);
            if (startTrace == null || endTrace == null || traceEnds.get(startTrace) == null || traceEnds.get(endTrace) == null) {
                continue;
            }
            boolean startForward = traceEnds.get(startTrace).getValue() == start;
            boolean endForward = traceEnds.get(endTrace).getKey() == end;
            Junction traceStart = startForward ? traceEnds.get(startTrace).getKey() : traceEnds.get(startTrace).getValue();
            Junction traceEnd = endForward ? traceEnds.get(endTrace).getValue() : traceEnds.get(endTrace).getKey();
            if (isRightAngle(startTrace, startForward, edge.getTrace(), true)
                    || isRightAngle(edge.getTrace(), true, endTrace, endForward)) {
                continue;
            }
            if (isLine(startTrace) && isLine(endTrace) && isLine(edge.getTrace())) {
                continue;
            }
            Trace joined = new Trace(new ArrayList<TracePoint>(startTrace.getPoints().size() + edge.getTrace().getPoints().size() + endTrace.getPoints().size()));
            joined.getPoints().addAll(startTrace.getPoints());
            if (!startForward) {
                Collections.reverse(joined.getPoints());
            }
            joined.getPoints().addAll(edge.getTrace().getPoints());
            joined.getPoints().addAll(endTrace.getPoints());
            if (!endForward) {
                Collections.reverse(joined.getPoints().subList(joined.getPoints().size() - endTrace.getPoints().size(), joined.getPoints().size()));
            }
            traceEnds.remove(startTrace);
            traceEnds.remove(endTrace);
            traceEnds.put(joined, new Pair<>(traceStart, traceEnd));
            jt.remove(start);
            jt.remove(end);
            if (jt.get(traceStart) != null) {
                jt.put(traceStart, joined);
            }
            if (jt.get(traceEnd) != null) {
                jt.put(traceEnd, joined);
            }
        }
    }

    private static double getTurning(Trace from, Segment bridge, Trace to, Map<Trace, Pair<Junction, Junction>> traceEnds, Graph<Junction, Segment> graph) {
        double angle0 = graph.getStart(bridge) == traceEnds.get(from).getValue()
                ? Segment.estimateEndAngle(from) : Segment.estimateStartAngle(from) + Math.PI;
        double angle1 = bridge.getAngleBegin();
        double angle2 = bridge.getAngleEnd();
        double angle3 = graph.getEnd(bridge) == traceEnds.get(to).getKey()
                ? Segment.estimateStartAngle(to) : Segment.estimateEndAngle(to) + Math.PI;
        return Math.abs(normalize(angle1 - angle0)) + Math.abs(normalize(angle3 - angle2));
    }

    private static boolean isRightAngle(Trace start, boolean startForward, Trace end, boolean endForward) {
        double startAngle = startForward ? Segment.estimateEndAngle(start) : Segment.estimateStartAngle(start) + Math.PI;
        double endAngle = endForward ? Segment.estimateStartAngle(end) : Segment.estimateEndAngle(end) + Math.PI;
        return Math.abs(Math.abs(normalize(endAngle - startAngle)) - Math.PI / 2) < Math.PI / 8;
    }

    private static boolean isLine(Trace trace) {
        long sumX = 0, sumY = 0, sumXX = 0, sumXY = 0, sumYY = 0, n = trace.getPoints().size();
        if (n <= 10) {
            return false;
        }
        int xmin = Integer.MAX_VALUE, ymin = Integer.MAX_VALUE, xmax = Integer.MIN_VALUE, ymax = Integer.MIN_VALUE;
        for (TracePoint point : trace.getPoints()) {
            int x = point.getX();
            int y = point.getY();
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
            sumYY += y * y;
            if (x < xmin) {
                xmin = x;
            }
            if (y < ymin) {
                ymin = y;
            }
            if (x > xmax) {
                xmax = x;
            }
            if (y > ymax) {
                ymax = y;
            }
        }
        long vx = n * sumXX - sumX * sumX;
        long vy = n * sumYY - sumY * sumY;
        if (vx == 0 || vy == 0) {
            return true;
        }
        long c = n * sumXY - sumX * sumY;
        long d = Math.min(vy - c * c / vx, vx - c * c / vy);
        long dd = (xmax - xmin + 1) * (xmax - xmin + 1) + (ymax - ymin + 1) * (ymax - ymin + 1);
        return d / n / n < dd / 4096;
    }

    private static double normalize(double angle) {
        if (Double.isNaN(angle)) {
            System.out.println("NaN");
            return Math.PI;
        } else {
            return angle - Math.round(angle / TWO_PI) * TWO_PI;
        }
    }
}
