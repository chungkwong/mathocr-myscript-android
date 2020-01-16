package cc.chungkwong.mathocr;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.myscript.iink.ContentPackage;
import com.myscript.iink.ContentPart;
import com.myscript.iink.ConversionState;
import com.myscript.iink.Editor;
import com.myscript.iink.Engine;
import com.myscript.iink.IEditorListener;
import com.myscript.iink.MimeType;
import com.myscript.iink.PointerEvent;
import com.myscript.iink.PointerEventType;
import com.myscript.iink.PointerType;
import com.myscript.iink.uireferenceimplementation.EditorView;
import com.myscript.iink.uireferenceimplementation.FontUtils;
import com.myscript.iink.uireferenceimplementation.InputController;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import cc.chungkwong.mathocr.extractor.BoundBox;
import cc.chungkwong.mathocr.extractor.Extractor;
import cc.chungkwong.mathocr.extractor.Trace;
import cc.chungkwong.mathocr.extractor.TraceList;
import cc.chungkwong.mathocr.extractor.TracePoint;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE = 0;
    private static final int REQUEST_CAMERA = 1;
    private static final long MOVE_GAP = 1, LEAVE_GAP = 1000;
    private Engine engine;
    private ContentPackage contentPackage;
    private ContentPart contentPart;
    private EditorView editorView;
    private TextView resultView;
    private MimeType type = MimeType.LATEX;
    private boolean whiteOnBlack = false;
    private Uri photoURI;
    private File photoFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ErrorActivity.installHandler(this);

        engine = MyscriptEngine.getEngine(getPackageCodePath(), getFilesDir().getPath());

        setContentView(R.layout.activity_main);

        editorView = findViewById(R.id.editor_view);
        resultView = findViewById(R.id.result);
        resultView.setMovementMethod(new ScrollingMovementMethod());
        // load fonts
        AssetManager assetManager = getApplicationContext().getAssets();
        Map<String, Typeface> typefaceMap = FontUtils.loadFontsFromAssets(assetManager);
        editorView.setTypefaces(typefaceMap);

        editorView.setEngine(engine);

        final Editor editor = editorView.getEditor();
        editor.addListener(new IEditorListener() {
            @Override
            public void partChanging(Editor editor, ContentPart oldPart, ContentPart newPart) {
                // no-op
            }

            @Override
            public void partChanged(Editor editor) {
                invalidateIconButtons();
            }

            @Override
            public void contentChanged(Editor editor, String[] blockIds) {
                invalidateIconButtons();
            }

            @Override
            public void onError(Editor editor, String blockId, String message) {
                Log.e(TAG, "Failed to edit block \"" + blockId + "\"" + message);
            }
        });

        setInputMode(InputController.INPUT_MODE_FORCE_PEN); // If using an active pen, put INPUT_MODE_AUTO here

        String packageName = "File1.iink";
        File file = new File(getFilesDir(), packageName);
        try {
            contentPackage = engine.createPackage(file);
            contentPart = contentPackage.createPart("Math"); // Choose type of content (possible values are: "Text Document", "Text", "Diagram", "Math", and "Drawing")
        } catch (IOException e) {
            Log.e(TAG, "Failed to open package \"" + packageName + "\"", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to open package \"" + packageName + "\"", e);
        }

        setTitle(getString(R.string.title_activity_main));

        // wait for view size initialization before setting part
        editorView.post(new Runnable() {
            @Override
            public void run() {
                editorView.getRenderer().setViewOffset(0, 0);
                editorView.getRenderer().setViewScale(1);
                editorView.setVisibility(View.VISIBLE);
                editor.setPart(contentPart);
            }
        });
        findViewById(R.id.button_undo).setOnClickListener(this);
        findViewById(R.id.button_redo).setOnClickListener(this);
        findViewById(R.id.button_clear).setOnClickListener(this);
        findViewById(R.id.button_input_mode_forcePen).setOnClickListener(this);
        findViewById(R.id.button_input_mode_forceTouch).setOnClickListener(this);

        findViewById(R.id.button_recognize).setOnClickListener(this);
        findViewById(R.id.button_file).setOnClickListener(this);
        findViewById(R.id.button_camera).setOnClickListener(this);
        invalidateIconButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (type == MimeType.MATHML) {
            menu.findItem(R.id.menu_mathml).setChecked(true);
        } else if (type == MimeType.JIIX) {
            menu.findItem(R.id.menu_jiix).setChecked(true);
        } else {
            menu.findItem(R.id.menu_latex).setChecked(true);
        }
        menu.findItem(R.id.menu_white_on_black).setChecked(whiteOnBlack);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_test: {
                startActivity(new Intent(this, TestActivity.class));
                return true;
            }
            case R.id.menu_batch: {
                startActivity(new Intent(this, BatchActivity.class));
                return true;
            }
            case R.id.menu_latex: {
                type = MimeType.LATEX;
                item.setChecked(true);
                recognize();
                return true;
            }
            case R.id.menu_mathml: {
                type = MimeType.MATHML;
                item.setChecked(true);
                recognize();
                return true;
            }
            case R.id.menu_jiix: {
                type = MimeType.JIIX;
                item.setChecked(true);
                recognize();
                return true;
            }
            case R.id.menu_white_on_black: {
                whiteOnBlack = !whiteOnBlack;
                item.setChecked(whiteOnBlack);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onDestroy() {
        editorView.setOnTouchListener(null);
        editorView.close();

        if (contentPart != null) {
            contentPart.close();
            contentPart = null;
        }
        if (contentPackage != null) {
            contentPackage.close();
            contentPackage = null;
        }


        // IInkApplication has the ownership, do not close here
        engine = null;

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_undo:
                editorView.getEditor().undo();
                break;
            case R.id.button_redo:
                editorView.getEditor().redo();
                break;
            case R.id.button_clear:
                editorView.getEditor().clear();
                resultView.setText("");
                break;
            case R.id.button_input_mode_forcePen:
                setInputMode(InputController.INPUT_MODE_FORCE_PEN);
                break;
            case R.id.button_input_mode_forceTouch:
                setInputMode(InputController.INPUT_MODE_FORCE_TOUCH);
                break;
            case R.id.button_recognize:
                recognize();
                break;
            case R.id.button_file:
                loadFile();
                break;
            case R.id.button_camera:
                loadCamera();
                break;
            default:
                Log.e(TAG, "Failed to handle click event");
                break;
        }
    }

    private void recognize() {
        Editor editor = editorView.getEditor();
        ConversionState[] supportedStates = editor.getSupportedTargetConversionStates(null);
        if (supportedStates.length > 0) {
            editor.convert(null, supportedStates[0]);
            editor.waitForIdle();
            try {
                String code = editor.export_(editor.getRootBlock(), type, null);
                resultView.setText(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE);
        }
    }

    private void loadCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile("formula_", ".jpg", directory);
                photoURI = FileProvider.getUriForFile(this,
                        "cc.chungkwong.mathocr",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_CAMERA);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE) {
                setBitmap(data.getData());
            } else if (requestCode == REQUEST_CAMERA) {
                setBitmap(photoURI);
                photoFile.delete();
            }
        }
    }

    private void setBitmap(Uri uri) {
        try {
            setBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setBitmap(Bitmap bitmap) {
        TraceList traceList = Extractor.extract(bitmap, whiteOnBlack);
        BoundBox box = traceList.getBoundBox();
        int left = box.getLeft();
        int top = box.getTop();
        Editor editor = editorView.getEditor();
        editor.clear();
        resultView.setText("");
        LinkedList<PointerEvent> events = new LinkedList<>();
        Long time = System.currentTimeMillis();
        for (Trace trace : traceList.getTraces()) {
            Iterator<TracePoint> iterator = trace.getPoints().iterator();
            if (iterator.hasNext()) {
                boolean multi = false;
                TracePoint point = iterator.next();
                events.add(new PointerEvent(PointerEventType.DOWN, point.getX() - left, point.getY() - top, time, 0, PointerType.PEN, -1));
                while (iterator.hasNext()) {
                    time += MOVE_GAP;
                    point = iterator.next();
                    events.add(new PointerEvent(PointerEventType.MOVE, point.getX() - left, point.getY() - top, time, 0, PointerType.PEN, -1));
                    multi = true;
                }
                PointerEvent last = events.getLast();
                if (multi) {
                    last.up(last.x, last.y);
                } else {
                    time += MOVE_GAP;
                    events.add(new PointerEvent(PointerEventType.UP, last.x, last.y, time, 0, PointerType.PEN, -1));
                }
                time += LEAVE_GAP;
            }
        }
        editor.pointerEvents(events.toArray(new PointerEvent[events.size()]), false);
    }

    private void setInputMode(int inputMode) {
        editorView.setInputMode(inputMode);
        findViewById(R.id.button_input_mode_forcePen).setEnabled(inputMode != InputController.INPUT_MODE_FORCE_PEN);
        findViewById(R.id.button_input_mode_forceTouch).setEnabled(inputMode != InputController.INPUT_MODE_FORCE_TOUCH);
    }

    private void invalidateIconButtons() {
        Editor editor = editorView.getEditor();
        final boolean canUndo = editor.canUndo();
        final boolean canRedo = editor.canRedo();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button buttonUndo = (Button) findViewById(R.id.button_undo);
                buttonUndo.setEnabled(canUndo);
                Button buttonRedo = (Button) findViewById(R.id.button_redo);
                buttonRedo.setEnabled(canRedo);
                Button buttonClear = (Button) findViewById(R.id.button_clear);
                buttonClear.setEnabled(contentPart != null);
            }
        });
    }
}
