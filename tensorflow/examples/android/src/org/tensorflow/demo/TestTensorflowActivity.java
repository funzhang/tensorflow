package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * @author zhangfan
 */
public class TestTensorflowActivity extends Activity {

    //    private static final String MODEL_FILE = "file:///android_asset/stylize_quantized.pb";
    private static final String MODEL_FILE = "file:///android_asset/optimized_3_128.pb";

    //    private static final String STYLE_NODE = "style_num";
    private static final String STYLE_NODE = "style";

    //    private static final int NUM_STYLES = 26;
    private static final int NUM_STYLES = 10;

    private static final String INPUT_NODE = "input";
    //    private static final String INPUT_NODE = "in";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
//    private static final String OUTPUT_NODE = "out";

    private boolean isDebug = false;


    private TensorFlowInferenceInterface inferenceInterface;

    private int frameNum = 0;

    private int desiredSize = 512;
    private int[] intValues;

    private float[] floatValues;

    private final float[] styleVals = new float[NUM_STYLES];

    private static final boolean DEBUG_MODEL = false;

    private Bitmap bitmap;

    private static final int REQUEST_CODE_PICK_IMAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intValues = new int[desiredSize * desiredSize];
        floatValues = new float[desiredSize * desiredSize * 3];

//        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);

        getImageFromAlbum();

    }

    private void getImageFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                bitmap = (Bitmap) bundle.get("data");

            } else {
                Uri uri = data.getData();
            }


            //to do find the path of pic by uri

        }
    }

    private boolean saveImage(Bitmap photo, String path) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(path, false));
            photo.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    private void stylizeImage(final Bitmap bitmap) {
        // ++frameNum;
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
//        inferenceInterface.feed(
//                INPUT_NODE, floatValues, 1, bitmap.getWidth(), bitmap.getHeight(), 3);
//        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
//        inferenceInterface.run(new String[]{OUTPUT_NODE}, isDebug);
//        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }

        bitmap.setPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

}
