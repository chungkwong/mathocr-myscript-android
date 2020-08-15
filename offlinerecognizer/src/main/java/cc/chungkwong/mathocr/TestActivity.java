package cc.chungkwong.mathocr;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;
import android.util.JsonReader;
import android.util.Log;
import android.widget.TextView;

import com.myscript.iink.Engine;
import com.myscript.iink.MimeType;
import com.myscript.iink.PointerEvent;
import com.myscript.iink.PointerEventType;
import com.myscript.iink.PointerType;
import com.myscript.iink.uireferenceimplementation.EditorView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TestActivity extends Activity implements Runnable {
    private static final long MOVE_GAP = 1, LEAVE_GAP = 1000;
    private TextView status;
    private EditorView editorView;
    private Engine engine;
    private boolean math;

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
        math=getIntent().getBooleanExtra("MATH_MODE",true);
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
        ZipOutputStream zipOut=null;
        Writer logOut=null;
        try {
            int processed = 0;
            long timeUsed=0;
            ZipInputStream zipIn=new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(Environment.getExternalStorageDirectory(), "/mathocr/in.zip"))));
            zipOut=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/mathocr/out.zip"))));
            logOut=new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/mathocr/out.log"))),"UTF-8");
            ZipEntry entry;
            while ((entry=zipIn.getNextEntry())!=null) {
                if(!entry.isDirectory()){
                    ++processed;
                    long startTime = SystemClock.currentThreadTimeMillis();
                    String[] results=MyscriptEngine.recognize(readInk(new JsonReader(new InputStreamReader(zipIn, "UTF-8"))), editorView,math,math?MimeType.LATEX:MimeType.TEXT,math?MimeType.MATHML:MimeType.JIIX);
                    timeUsed+=(SystemClock.currentThreadTimeMillis()-startTime);
                    logOut.write(entry.getName());
                    logOut.write('\t');
                    logOut.write(results[0].replaceAll("\\s+"," "));
                    logOut.write('\n');
                    zipOut.putNextEntry(new ZipEntry(entry.getName()));
                    zipOut.write(results[1].getBytes("UTF-8"));
                    zipOut.closeEntry();
                    runOnUiThread(new Prompt(processed +"("+timeUsed+"ms)"));
//                        Thread.sleep(500);
                }
                zipIn.closeEntry();
            }
            zipOut.finish();
            zipOut.close();
            zipIn.close();
            runOnUiThread(new Prompt("Finished("+timeUsed+"ms)"));
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Prompt("Error: " + e.getMessage()));
        }finally {
            if(zipOut!=null) {
                try {
                    zipOut.finish();
                    zipOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(logOut!=null) {
                try {
                    logOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
                String value = MyscriptEngine.recognize(readInk(jsonReader), editorView,math,math?MimeType.LATEX:MimeType.TEXT)[0];
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
