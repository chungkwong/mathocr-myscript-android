package cc.chungkwong.mathocr.extractor;

import java.util.LinkedList;

/**
 * Segment
 *
 * @author Chan Chung Kwong
 */
public class Segment extends Component {
//    private static final double TURN_ANGLE_THREHOLD = Math.PI * 5 / 6;
    private double angleBegin, angleEnd;

    /**
     * Create a empty segment
     */
    public Segment() {
        super(new Trace(new LinkedList<TracePoint>()));
    }

    /**
     * Create a segment
     *
     * @param trace points
     * @param thick thickness
     */
    public Segment(Trace trace, int thick) {
        this(trace, thick, estimateStartAngle(trace,thick), estimateEndAngle(trace,thick));
    }

    /**
     * Create a segment
     *
     * @param trace      points
     * @param thick      thickness
     * @param angleBegin start angle
     * @param angleEnd   end angle
     */
    public Segment(Trace trace, int thick, double angleBegin, double angleEnd) {
        super(trace);
        setThick(thick);
        this.angleBegin = angleBegin;
        this.angleEnd = angleEnd;
    }

    static double estimateStartAngle(Trace trace,double thick) {
//		int mid=trace.getPoints().size();
//		while(mid>=3&&calculateAngle(trace.getPoints().get(0),trace.getPoints().get(mid/2),trace.getPoints().get(mid-1))<TURN_ANGLE_THREHOLD){
//			mid/=2;
//		}
//		return calculateAngle(trace.getPoints().get(0),trace.getPoints().get(mid-1));
        int r=Math.max((int)(Math.sqrt(thick)*2),5);
        TracePoint start = trace.getStart();
        TracePoint end = trace.getPoints().size() <= r ? trace.getEnd() : trace.getPoints().get(r);
        return calculateAngle(start, end);
    }

    static double estimateEndAngle(Trace trace,double thick) {
//		int len=trace.getPoints().size();
//		int mid=trace.getPoints().size();
//		while(mid>=3&&calculateAngle(trace.getPoints().get(len-mid),trace.getPoints().get(len-1-mid/2),trace.getPoints().get(len-1))<TURN_ANGLE_THREHOLD){
//			mid/=2;
//		}
//		return calculateAngle(trace.getPoints().get(len-mid),trace.getPoints().get(len-1));
        int r=Math.max((int)(Math.sqrt(thick)*2),5);
        TracePoint start = trace.getPoints().size() <= r ? trace.getStart() : trace.getPoints().get(trace.getPoints().size() - r);
        TracePoint end = trace.getEnd();
        return calculateAngle(start, end);
    }

    //	private static double calculateAngle(TracePoint p,TracePoint q,TracePoint r){
//		double a=Math.hypot(r.getX()-p.getX(),r.getY()-p.getY());
//		double b=Math.hypot(q.getX()-p.getX(),q.getY()-p.getY());
//		double c=Math.hypot(r.getX()-q.getX(),r.getY()-q.getY());
//		return Math.acos((b*b+c*c-a*a)/(2*b*c));
//	}
    private static double calculateAngle(TracePoint p, TracePoint q) {
        int dx = q.getX() - p.getX();
        int dy = q.getY() - p.getY();
        return Math.atan2(dy, dx);
    }

    /**
     * @return start angle
     */
    public double getAngleBegin() {
        return angleBegin;
    }

    /**
     * Set start angle
     *
     * @param angleBegin
     */
    public void setAngleBegin(double angleBegin) {
        this.angleBegin = angleBegin;
    }

    /**
     * @return end angle
     */
    public double getAngleEnd() {
        return angleEnd;
    }

    /**
     * Set end angle
     *
     * @param angleEnd
     */
    public void setAngleEnd(double angleEnd) {
        this.angleEnd = angleEnd;
    }

    void updateAngles() {
        this.angleBegin = estimateStartAngle(getTrace(),getThick());
        this.angleEnd = estimateEndAngle(getTrace(),getThick());
    }
}
