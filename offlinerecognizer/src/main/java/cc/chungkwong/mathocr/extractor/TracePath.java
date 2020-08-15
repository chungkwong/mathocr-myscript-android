//package cc.chungkwong.mathocr.extractor;
//
//import android.util.Log;
//
//import com.myscript.iink.graphics.IPath;
//
//import java.util.ArrayList;
//import java.util.EnumSet;
//import java.util.List;
//
//public class TracePath implements IPath {
//    private final List<Trace> traceList=new ArrayList<Trace>();
//    @Override
//    public EnumSet<OperationType> unsupportedOperations() {
//        return EnumSet.of(OperationType.ARC_OPS,OperationType.CURVE_OPS,OperationType.QUAD_OPS);
//    }
//
//    @Override
//    public void moveTo(float v, float v1) {
//        Trace trace=new Trace();
//        trace.getPoints().add(new TracePoint(((int)v*1000),((int)v1*1000)));
//        traceList.add(trace);
//    }
//
//    @Override
//    public void lineTo(float v, float v1) {
//        traceList.get(traceList.size()-1).getPoints().add(new TracePoint(((int)v*1000),((int)v1*1000)));
//    }
//
//    @Override
//    public void curveTo(float v, float v1, float v2, float v3, float v4, float v5) {
//        Log.w("mathocr","curve to");
//    }
//
//    @Override
//    public void quadTo(float v, float v1, float v2, float v3) {
//        Log.w("mathocr","quad to");
//    }
//
//    @Override
//    public void arcTo(float v, float v1, float v2, boolean b, boolean b1, float v3, float v4) {
//        Log.w("mathocr","arc to");
//    }
//
//    @Override
//    public void closePath() {
//        Log.w("mathocr","close path");
//    }
//
//    public List<Trace> getTraceList() {
//        return traceList;
//    }
//}
