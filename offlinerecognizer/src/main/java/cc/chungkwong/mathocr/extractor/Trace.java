package cc.chungkwong.mathocr.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Stroke
 *
 * @author Chan Chung Kwong
 */
public class Trace {
    private final List<TracePoint> points;
    private final String id;
    private BoundBox box;

    /**
     * Create a empty stroke
     */
    public Trace() {
        this(new ArrayList<TracePoint>());
    }

    /**
     * Create a stroke
     *
     * @param points underlying points
     */
    public Trace(List<TracePoint> points) {
        this(points, null);
    }

    /**
     * Create a empty stroke
     *
     * @param id ID
     */
    public Trace(String id) {
        this(new ArrayList<TracePoint>(), id);
    }

    /**
     * Create a stroke
     *
     * @param points underlying points
     * @param id     ID
     */
    public Trace(List<TracePoint> points, String id) {
        this.points = points;
        this.id = id;
    }

    /**
     * @return underlying points
     */
    public List<TracePoint> getPoints() {
        return points;
    }

    /**
     * @return first point
     */
    public TracePoint getStart() {
        return points.get(0);
    }

    /**
     * @return last point
     */
    public TracePoint getEnd() {
        return points.get(points.size() - 1);
    }

    /**
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return bounding box of the stroke(cached)
     */
    public BoundBox getBoundBox() {
        if (box == null) {
            int maxX = Integer.MIN_VALUE, minX = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
            for (TracePoint point : getPoints()) {
                int x = point.getX();
                int y = point.getY();
                if (x < minX) {
                    minX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
            box = new BoundBox(minX, maxX, minY, maxY);
        }
        return box;
    }

    /**
     * Clear cached bounding box
     */
    public void invalidBoundBox() {
        box = null;
    }

    @Override
    public String toString() {
        return points.toString();
    }
}