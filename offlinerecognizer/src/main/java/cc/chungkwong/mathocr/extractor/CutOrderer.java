package cc.chungkwong.mathocr.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CutOrderer {
    public static TraceList order(TraceList traceList) {
        flip(traceList.getTraces());
        ArrayList<Trace> traces = new ArrayList<>(traceList.getTraces());
        hSort(traces);
        return new TraceList(traces);
    }

    private static void hSort(List<Trace> traces) {
        Collections.sort(traces, new Comparator<Trace>() {
            @Override
            public int compare(Trace t0, Trace t1) {
                return t0.getBoundBox().getLeft() - t1.getBoundBox().getLeft();
            }
        });
        for (int i = 0; i < traces.size(); i++) {
            int lastx = traces.get(i).getBoundBox().getRight();
            int j = i + 1;
            while (j < traces.size() && traces.get(j).getBoundBox().getLeft() <= lastx) {
                lastx = Math.max(lastx, traces.get(j).getBoundBox().getRight());
                ++j;
            }
            if (j > i + 1) {
                vSort(traces.subList(i, j));
            }
            i = j - 1;
        }
    }

    private static void vSort(List<Trace> traces) {
        Collections.sort(traces, new Comparator<Trace>() {
            @Override
            public int compare(Trace t0, Trace t1) {
                return t0.getBoundBox().getTop() - t1.getBoundBox().getTop();
            }
        });
        for (int i = 0; i < traces.size(); i++) {
            int lasty = traces.get(i).getBoundBox().getBottom();
            int j = i + 1;
            while (j < traces.size() && traces.get(j).getBoundBox().getTop() <= lasty) {
                lasty = Math.max(lasty, traces.get(j).getBoundBox().getBottom());
                ++j;
            }
            if (j > i + 1) {
                if (i != 0 || j != traces.size()) {
                    hSort(traces.subList(i, j));
                } else {
                    List<Trace> order = TopologicalOrderer.order(new TraceList(traces.subList(i, j))).getTraces();
                    for (int k = 0; k < order.size(); k++) {
                        traces.set(i + k, order.get(k));
                    }
                }
            }
            i = j - 1;
        }
    }

    private static void flip(List<Trace> traces) {
        for (Trace trace : traces) {
            List<TracePoint> points = trace.getPoints();
            if (points.isEmpty()) {
                continue;
            }
            TracePoint first = points.get(0);
            TracePoint last = points.get(points.size() - 1);
            if (2 * last.getX() + 3 * last.getY() < 2 * first.getX() + 3 * first.getY()) {
                Collections.reverse(points);
            }
        }
    }

}
