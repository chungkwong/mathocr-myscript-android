package cc.chungkwong.mathocr;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.JsonReader;
import android.util.Log;
import android.widget.TextView;

import com.myscript.iink.Engine;
import com.myscript.iink.PointerEvent;
import com.myscript.iink.PointerEventType;
import com.myscript.iink.PointerType;
import com.myscript.iink.uireferenceimplementation.EditorView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;

public class TestActivity extends Activity implements Runnable {
    private static final long MOVE_GAP = 1, LEAVE_GAP = 1000;
    private TextView status;
    private EditorView editorView;
    private Engine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ErrorActivity.installHandler(this);
        engine = MyscriptEngine.getEngine(getPackageCodePath(), getFilesDir().getPath());
        setContentView(R.layout.activity_test);
        status = findViewById(R.id.status);
        editorView = new EditorView(this);
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
        new Thread(this).start();
    }

    @Override
    protected void onDestroy() {
        engine = null;
        editorView.close();
        MyscriptEngine.clearUp();
        super.onDestroy();
    }

    @Override
    public void run() {
        testFormulas();
        //testSymbols();
    }

    private void testFormulas() {
        File inDir = new File(Environment.getExternalStorageDirectory(), "/mathocr/in");
        File outDir = new File(Environment.getExternalStorageDirectory(), "/mathocr/out");
        outDir.mkdirs();
        File[] files = inDir.listFiles();
        final int total = files.length;
        int processed = 0;
        long timeUsed=0;
        try {
            for (File in : files) {
                ++processed;
                Log.e("mathocr", in.getCanonicalPath());
                if (in.getName().endsWith(".json")) {
                    File out = new File(outDir, in.getName().replace(".json", ".jiix"));
                    Log.e("mathocr", out.getCanonicalPath());
                    if (out.exists()) {
                        continue;
                    }
                    long startTime = SystemClock.currentThreadTimeMillis();
                    testFile(in, out);
                    timeUsed+=(SystemClock.currentThreadTimeMillis()-startTime);
                    runOnUiThread(new Prompt(processed + "/" + total+"("+timeUsed+"ms)"));
                    Thread.sleep(500);
                }
            }
            runOnUiThread(new Prompt("Finished("+timeUsed+"ms)"));
        } catch (Exception ex) {
            runOnUiThread(new Prompt("Error: " + ex.getMessage()));
            ex.printStackTrace();
        }
    }

    private void testFile(File file, File result) throws IOException {
        MyscriptEngine.recognize(readInk(file), result, editorView);
    }

    private void testSymbols() {
        File in = new File(Environment.getExternalStorageDirectory(), "/mathocr/single2014.json");
        File out = new File(Environment.getExternalStorageDirectory(), "/mathocr/single2014.csv");
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(in));
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
            jsonReader.beginObject();
            int processed = 0;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                String value = MyscriptEngine.recognize(readInk(jsonReader), editorView);
                writer.write(name);
                writer.write(',');
                writer.write(value);
                writer.write('\n');
                runOnUiThread(new Prompt(++processed + ":" + name + ":" + value));
                Thread.sleep(50);
            }
            jsonReader.endObject();
            writer.close();
            jsonReader.close();
        } catch (Exception e) {
            runOnUiThread(new Prompt("Error: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private PointerEvent[] readInk(File file) throws IOException {
        JsonReader jsonReader = new JsonReader(new FileReader(file));
        PointerEvent[] pointerEvents = readInk(jsonReader);
        jsonReader.close();
        return pointerEvents;
    }

    private PointerEvent[] readInk(JsonReader jsonReader) throws IOException {
        LinkedList<PointerEvent> events = new LinkedList<>();
        jsonReader.beginArray();
        Long time = System.currentTimeMillis();
        while (jsonReader.hasNext()) {
            jsonReader.beginArray();
            if (jsonReader.hasNext()) {
                boolean multi = false;
                events.add(new PointerEvent(PointerEventType.DOWN, jsonReader.nextInt(), jsonReader.nextInt(), time, 0, PointerType.PEN, -1));
                while (jsonReader.hasNext()) {
                    time += MOVE_GAP;
                    events.add(new PointerEvent(PointerEventType.MOVE, jsonReader.nextInt(), jsonReader.nextInt(), time, 0, PointerType.PEN, -1));
                    multi = true;
                }
                PointerEvent last = events.getLast();
                if (multi) {
                    last.up(last.x, last.y);
                } else {
                    time += MOVE_GAP;
                    events.add(new PointerEvent(PointerEventType.UP, last.x, last.y, time, 0, PointerType.PEN, -1));
                }
            }
            jsonReader.endArray();
            time += LEAVE_GAP;
        }
        jsonReader.endArray();
        return events.toArray(new PointerEvent[events.size()]);
    }

    private class Prompt implements Runnable {
        private final String message;

        public Prompt(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            status.setText(message);
        }
    }
}
