package cc.chungkwong.mathocr;

import android.graphics.Typeface;
import android.util.DisplayMetrics;

import com.myscript.iink.Configuration;
import com.myscript.iink.ContentPackage;
import com.myscript.iink.ContentPart;
import com.myscript.iink.Editor;
import com.myscript.iink.Engine;
import com.myscript.iink.MimeType;
import com.myscript.iink.PointerEvent;
import com.myscript.iink.Renderer;
import com.myscript.iink.uireferenceimplementation.EditorView;
import com.myscript.iink.uireferenceimplementation.FontMetricsProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyscriptEngine {
    private static Engine engine;
    private static Editor editor;
    private static ContentPackage pkg;

    public static synchronized Engine getEngine(String pkgPath, String filePath) {
        if (engine == null) {
            engine = Engine.create(MyCertificate.getBytes());
            Configuration conf = engine.getConfiguration();
            String confDir = "zip://" + pkgPath + "!/assets/conf";
            conf.setStringArray("configuration-manager.search-path", new String[]{confDir});
            String tempDir = filePath + File.separator + "tmp";
            conf.setString("content-package.temp-folder", tempDir);
            conf.setBoolean("text.guides.enable", false);
            conf.setBoolean("export.jiix.strokes", false);
            conf.setBoolean("gesture.enable", false);
//            conf.setString("math.configuration.name", "crohme");
//            conf.setString("math.configuration.name", "standard");
//            conf.setString("math.configuration.name", "small");
            conf.setString("math.configuration.name", "mini");
//            conf.setString("math.configuration.name", "single");
            conf.setNumber("math.solver.fractional-part-digits", 15);
            conf.setBoolean("math.solver.enable", false);
        }
        return engine;
    }

    public static synchronized Editor getEditor(EditorView widget) {
        if (editor == null) {
            float dpiX = 384;
            float dpiY = 384;
//            float dpiX = 576;
//            float dpiY = 576;
//            float dpiX = 192;
//            float dpiY = 192;
            Renderer renderer = engine.createRenderer(dpiX, dpiY, null);
            editor = engine.createEditor(renderer);
            DisplayMetrics displayMetrics = widget.getResources().getDisplayMetrics();
            Map<String, Typeface> typefaceMap = new HashMap<>();
            editor.setFontMetricsProvider(new FontMetricsProvider(displayMetrics, typefaceMap));
            editor.setViewSize(1010, 1010);
//            editor.setViewSize(38, 38);
            try {
                pkg = engine.createPackage("ttmp.iink");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return editor;
    }

    public static synchronized String recognize(PointerEvent[] events, EditorView widget) throws IOException {
        Editor editor = getEditor(widget);
        ContentPart part = pkg.createPart("Math");
        editor.setPart(part);
        editor.pointerEvents(events, false);
        editor.convert(null, editor.getSupportedTargetConversionStates(null)[0]);
        editor.waitForIdle();
        String string = editor.export_(editor.getRootBlock(), MimeType.LATEX, null);
        pkg.removePart(part);
        part.close();
        return string;
    }

    public static synchronized void recognize(PointerEvent[] events, File result, EditorView widget) throws IOException {
        Editor editor = getEditor(widget);
        ContentPart part = pkg.createPart("Math");
        editor.setPart(part);
        editor.pointerEvents(events, false);
        editor.convert(null, editor.getSupportedTargetConversionStates(null)[0]);
        editor.waitForIdle();
        //editor.export_(editor.getRootBlock(),result.getCanonicalPath(), MimeType.JIIX,null);
        editor.export_(editor.getRootBlock(), result.getCanonicalPath(), MimeType.MATHML, null);
        pkg.removePart(part);
        part.close();
    }

    public static void clearUp() {
        editor.close();
        editor = null;
        pkg.close();
        try {
            engine.deletePackage("ttmp.iink");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
