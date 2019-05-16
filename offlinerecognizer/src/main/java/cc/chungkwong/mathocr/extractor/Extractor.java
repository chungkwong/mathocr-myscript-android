package cc.chungkwong.mathocr.extractor;

import android.graphics.Bitmap;

public class Extractor {
    private static final Binarizer BINARIZER = new Binarizer(0.11, 128);

    public static TraceList extract(Bitmap bitmap, boolean whiteOnBlack) {
        Raster raster = BINARIZER.apply(bitmap, whiteOnBlack);
        TraceList traces = GraphTracer.trace(ThinTracer.trace(raster));
        return CutOrderer.order(traces);
    }
}
