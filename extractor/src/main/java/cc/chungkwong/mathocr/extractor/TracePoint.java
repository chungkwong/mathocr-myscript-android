package cc.chungkwong.mathocr.extractor;

/**
 * Point
 *
 * @author Chan Chung Kwong
 */
public class TracePoint {
    private final int x, y;

    /**
     * Create a point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public TracePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @param index index of the point in the image
     * @param width width of the image
     * @return the point
     */
    public static TracePoint fromIndex(int index, int width) {
        return new TracePoint(index % width, index / width);
    }

    public static int getDistanceSquare(TracePoint p, TracePoint q) {
        int dx = p.getX() - q.getX();
        int dy = p.getY() - q.getY();
        return dx * dx + dy * dy;
    }

    /**
     * @return the x coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * @param width width of the image
     * @return index of the point in the image
     */
    public int toIndex(int width) {
        return y * width + x;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TracePoint && ((TracePoint) obj).x == x && ((TracePoint) obj).y == y;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + this.x;
        hash = 47 * hash + this.y;
        return hash;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}