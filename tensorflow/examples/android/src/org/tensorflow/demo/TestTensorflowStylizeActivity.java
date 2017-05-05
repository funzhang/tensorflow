package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * @author zhangfan
 */
public class TestTensorflowStylizeActivity extends Activity {

    private static final String TAG = TestTensorflowStylizeActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final String BITMAP_SAVE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final boolean DEBUG_MODEL = false;

    private static final String MODEL_FILE = "file:///android_asset/inference_256.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 32;

    private TensorFlowInferenceInterface inferenceInterface;
    private int desiredSize = 256;
    private int frameNum = 0;
    private final float[] styleVals = new float[NUM_STYLES];
    private int[] intValues;
    private float[] floatValues;

    private int[] rgbBytes = null;
    private Bitmap rgbFrameBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private ImageView src;
    private ImageView dst;

    private Bitmap mutableBitmap = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_tensorflow);

        Button button = (Button) findViewById(R.id.pick_image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        src = (ImageView) findViewById(R.id.src);
        dst = (ImageView) findViewById(R.id.dst);

//        cropToFrameTransform = new Matrix();
//        frameToCropTransform.invert(cropToFrameTransform);


        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        styleVals[0] = 1.0f;


    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_PICK_IMAGE == requestCode) {
            Bitmap bitmap = null;
            Bundle bundle = data.getExtras();
            if (null != bundle) {
                Object obj = bundle.get("data");
                if (null != obj) {
                    Log.d(TAG, "Get SRC bitmap from bundle!");
                    bitmap = (Bitmap) obj;
                } else {
                    Uri uri = data.getData();
                    Log.d(TAG, "Get SRC bitmap by uri: " + uri);
                    bitmap = getBitmapByUri(uri);
                }
            }
            if (null != bitmap) {
                src.setImageBitmap(bitmap);
                doStylize(bitmap);
            } else {
                Log.e(TAG, "SRC bitmap is null!");
            }
        }
    }

    private Bitmap getBitmapByUri(Uri uri) {
        Bitmap bitmap = null;
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            Log.d(TAG, "SRC bitmap picturePath: " + picturePath);
            bitmap = BitmapFactory.decodeFile(picturePath);
        } catch (Exception e) {
            try {
                cursor.close();
            } catch (Exception e1) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    private void doStylize(Bitmap bitmap) {
        StylizeTask task = new StylizeTask();
        task.execute(bitmap);
    }

    private class StylizeTask extends AsyncTask<Bitmap, Integer, Bitmap> {

        long startTime;

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Start to stylize the bitmap!", Toast.LENGTH_SHORT).show();
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Bitmap bitmap = null;
            try {
                bitmap = stylizeImage(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (null != bitmap) {
                long cost = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Operation cost = " + cost);
                dst.setImageBitmap(bitmap);
                saveImage(bitmap);
            } else {
                Log.e(TAG, "Bitmap is null!");
            }
        }
    }

    private void saveImage(Bitmap bitmap) {
        BufferedOutputStream bos = null;
        try {
            String path = BITMAP_SAVE_PATH + "/" + System.currentTimeMillis();
            bos = new BufferedOutputStream(
                    new FileOutputStream(path, false));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            Toast.makeText(getApplicationContext(), "Save to path : \n" + path, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap stylizeImage(Bitmap bitmap) {
//        int previewWidth = bitmap.getWidth();
//        int previewHeight = bitmap.getHeight();
//        Log.d(TAG, "Initializing at size preview size " + previewWidth + "x" + previewHeight + ", stylize size " + desiredSize);
//
//        int sensorOrientation = 0;
//
//        rgbBytes = new int[previewWidth * previewHeight];
//        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
//        croppedBitmap = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888);
//
//        frameToCropTransform =
//                ImageUtils.getTransformationMatrix(
//                        previewWidth, previewHeight,
//                        desiredSize, desiredSize,
//                        sensorOrientation, true);
//
//        cropToFrameTransform = new Matrix();
//        frameToCropTransform.invert(cropToFrameTransform);
//
//
//
//        yuvBytes = new byte[3][];
//
//        yuvBytes = Bmp2YUV.getYUV420sp(previewWidth,previewHeight,bitmap);
//
//        intValues = new int[desiredSize * desiredSize];
//        floatValues = new float[desiredSize * desiredSize * 3];

        Log.d(TAG, "BITMAP width = " + bitmap.getWidth());
        Log.d(TAG, "BITMAP height = " + bitmap.getHeight());

        desiredSize = bitmap.getWidth();

        intValues = new int[desiredSize * desiredSize];
        floatValues = new float[desiredSize * desiredSize * 3];

        mutableBitmap = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888);

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        if (DEBUG_MODEL) {
            // Create a white square that steps through a black background 1 pixel per frame.
            final int centerX = (frameNum + bitmap.getWidth() / 2) % bitmap.getWidth();
            final int centerY = bitmap.getHeight() / 2;
            final int squareSize = 10;
            for (int i = 0; i < intValues.length; ++i) {
                final int x = i % bitmap.getWidth();
                final int y = i / bitmap.getHeight();
                final float val =
                        Math.abs(x - centerX) < squareSize && Math.abs(y - centerY) < squareSize ? 1.0f : 0.0f;
                floatValues[i * 3] = val;
                floatValues[i * 3 + 1] = val;
                floatValues[i * 3 + 2] = val;
            }
        } else {
            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
                floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
            }
        }

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(
                INPUT_NODE, floatValues, 1, bitmap.getWidth(), bitmap.getHeight(), 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        inferenceInterface.run(new String[]{OUTPUT_NODE}, true);
        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }

        mutableBitmap.setPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return mutableBitmap;
    }


}
