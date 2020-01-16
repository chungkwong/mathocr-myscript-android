package cc.chungkwong.mathocr;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Debug;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
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
import java.util.Iterator;
import java.util.LinkedList;

import cc.chungkwong.mathocr.extractor.Extractor;
import cc.chungkwong.mathocr.extractor.Trace;
import cc.chungkwong.mathocr.extractor.TraceList;
import cc.chungkwong.mathocr.extractor.TracePoint;

public class BatchActivity extends Activity  implements Runnable {

    private static final long MOVE_GAP = 1, LEAVE_GAP = 1000;
    private TextView status;
    private EditorView editorView;
    private Engine engine;
    private DocumentFile directory;
    private File directoryOld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ErrorActivity.installHandler(this);
        engine = MyscriptEngine.getEngine(getPackageCodePath(), getFilesDir().getPath());
        setContentView(R.layout.activity_batch);
        status = findViewById(R.id.batch_status);
        editorView = new EditorView(this);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if(Build.VERSION.SDK_INT>=21) {
            startActivityForResult(intent, REQUEST_DIRECTORY);
        }else{
            directoryOld=new File(new File(Environment.getExternalStorageDirectory(),Environment.DIRECTORY_DOWNLOADS),"crohme2016");
            new Thread(this).start();
        }
    }
    private static final int REQUEST_DIRECTORY = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_DIRECTORY) {
                directory=DocumentFile.fromTreeUri(this,data.getData());
                new Thread(this).start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        engine = null;
        editorView.close();
        super.onDestroy();
    }

    @Override
    public void run() {
        if(Build.VERSION.SDK_INT>=21) {
            testFormulasNew();
        }else{
            testFormulasOld();
        }
        //testSymbols();
    }

    private void testFormulasNew() {
        DocumentFile[] files = directory.listFiles();
        final int total = files.length;
        int processed = 0;
        long timeUsedExtract=0;
        long timeUsedRecognize=0;
        StringBuilder buf=new StringBuilder();
        try {
            for (DocumentFile in : files) {
                Log.v("mathocr",in.getUri().toString());
                ++processed;
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), in.getUri());
                long startTime=System.nanoTime();
                TraceList traceList = Extractor.extract(bitmap, false);
                timeUsedExtract+=(System.nanoTime()-startTime);
                PointerEvent[] events = wrap(traceList);
                startTime = System.nanoTime();
                String latex=MyscriptEngine.recognize(events, editorView);
                timeUsedRecognize+=(System.nanoTime()-startTime);
                buf.append(latex).append('\n');
                runOnUiThread(new Prompt(latex+"\n"+processed + "/" + total+"("+timeUsedExtract/1e6+"+"+timeUsedRecognize/1e6+"ms)"));
            }
            runOnUiThread(new Prompt("Finished "+processed+"("+timeUsedExtract/1e6+"+"+timeUsedRecognize/1e6+"ms)\n"+
                    Runtime.getRuntime().totalMemory()/1024/1024+"+"+Debug.getNativeHeapAllocatedSize()/1024/1024+"MiB\n"+buf.toString()));
        } catch (Exception ex) {
            runOnUiThread(new Prompt("Error: " + ex.getMessage()));
            ex.printStackTrace();
        }
    }
    private void testFormulasOld() {
        File[] files = directoryOld.listFiles();
        final int total = files.length;
        int processed = 0;
        long timeUsedExtract=0;
        long timeUsedRecognize=0;
        StringBuilder buf=new StringBuilder();
        try {
            for (File in : files) {
                Log.v("mathocr",in.toString());
                ++processed;
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(in));
                long startTime=System.nanoTime();
                TraceList traceList = Extractor.extract(bitmap, false);
                timeUsedExtract+=(System.nanoTime()-startTime);
                PointerEvent[] events = wrap(traceList);
                startTime = System.nanoTime();
                String latex=MyscriptEngine.recognize(events, editorView);
                timeUsedRecognize+=(System.nanoTime()-startTime);
                buf.append(latex).append('\n');
                runOnUiThread(new Prompt(latex+"\n"+processed + "/" + total+"("+timeUsedExtract/1e6+"+"+timeUsedRecognize/1e6+"ms)"));
            }
            runOnUiThread(new Prompt("Finished "+processed+"("+timeUsedExtract/1e6+"+"+timeUsedRecognize/1e6+"ms)\n"+
                    Runtime.getRuntime().totalMemory()/1024/1024+"+"+Debug.getNativeHeapAllocatedSize()/1024/1024+"MiB\n"+buf.toString()));
        } catch (Exception ex) {
            runOnUiThread(new Prompt("Error: " + ex.getMessage()));
            ex.printStackTrace();
        }
    }
    private PointerEvent[] wrap(TraceList traceList) throws IOException {
        LinkedList<PointerEvent> events = new LinkedList<>();
        Long time = System.currentTimeMillis();
        for(Trace trace:traceList.getTraces()){
            Iterator<TracePoint> iterator = trace.getPoints().iterator();
            if(iterator.hasNext()) {
                TracePoint curr = iterator.next();
                events.add(new PointerEvent(PointerEventType.DOWN, curr.getX(), curr.getY(), time, 0, PointerType.PEN, -1));
                if(iterator.hasNext()) {
                    while (iterator.hasNext()) {
                        time += MOVE_GAP;
                        curr = iterator.next();
                        if (iterator.hasNext())
                            events.add(new PointerEvent(PointerEventType.MOVE, curr.getX(), curr.getY(), time, 0, PointerType.PEN, -1));
                        else
                            events.add(new PointerEvent(PointerEventType.UP, curr.getX(), curr.getY(), time, 0, PointerType.PEN, -1));
                    }
                }else{
                    events.add(new PointerEvent(PointerEventType.UP, curr.getX(), curr.getY(), time, 0, PointerType.PEN, -1));
                }
                time+=LEAVE_GAP;
            }
        }
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
