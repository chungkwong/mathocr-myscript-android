package cc.chungkwong.mathocr.extractor;

/**
 * Connected points
 *
 * @author Chan Chung Kwong
 */
public class Component {
    private final Trace trace;
    private int thick;

    /**
     * Create a component
     *
     * @param trace points
     */
    public Component(Trace trace) {
        this.trace = trace;
    }

    /**
     * @return points
     */
    public Trace getTrace() {
        return trace;
    }

    /**
     * @return thick
     */
    public int getThick() {
        return thick;
    }

    /**
     * Set thickness
     *
     * @param thick thickness
     */
    public void setThick(int thick) {
        this.thick = thick;
    }
}
