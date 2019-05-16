package cc.chungkwong.mathocr.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequence of strokes
 *
 * @author Chan Chung Kwong
 */
public class TraceList {
    private final List<Trace> traces;

    /**
     * Create a empty sequence of strokes
     */
    public TraceList() {
        this.traces = new ArrayList<>();
    }

    /**
     * Create a sequence of strokes
     *
     * @param traces underlying list
     */
    public TraceList(List<Trace> traces) {
        this.traces = traces;
    }

    /**
     * @return list of strokes
     */
    public List<Trace> getTraces() {
        return traces;
    }

    /**
     * @return bounding box of the strokes
     */
    public BoundBox getBoundBox() {
        ArrayList<BoundBox> boxes = new ArrayList<>(traces.size());
        for (Trace trace : traces)
            boxes.add(trace.getBoundBox());
        return BoundBox.union(boxes.iterator());
    }

    @Override
    public String toString() {
        return traces.toString();
    }
}