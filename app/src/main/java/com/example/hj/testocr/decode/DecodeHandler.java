package com.example.hj.testocr.decode;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.hj.testocr.activity.CaptureActivity;
import com.example.hj.testocr.R;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecodeHandler extends Handler {

    private final CaptureActivity activity;
    private boolean running = true;


    public  DecodeHandler(CaptureActivity activity) {
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case R.id.decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    private void decode(byte[] data, int width, int height) {
        Camera.Size size = activity.getCameraManager().getPreviewSize();

        Handler handler = activity.getHandler();

        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++)
                rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
        }

        // 宽高也要调整
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;
        PlanarYUVLuminanceSource source = buildLuminanceSource(rotatedData, size.width, size.height);

        Bitmap bitmap = renderCroppedGreyscaleBitmap(rotatedData, size.width, size.height, source);

        String rawResult = getPhoneNumber(bitmap);


        if (rawResult != null) {

            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);

                Bundle bundle = new Bundle();
                bundleThumbnail(source, bundle);
                message.setData(bundle);

                message.sendToTarget();

            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }

    }

    private String getPhoneNumber(Bitmap bitmap) {
        TessBaseAPI baseApi = activity.getBaseAPI();
        baseApi.setImage(ReadFile.readBitmap(bitmap));
        String textResult = baseApi.getUTF8Text();
        baseApi.clear();
        baseApi.end();
        Pattern r = Pattern.compile(".*\\d+.*");
        Matcher m = r.matcher(textResult);
        if (m.find()) {
            return textResult;
        }
        return null;
    }


    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    }


    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = activity.getCropRect();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
    }

    public Bitmap renderCroppedGreyscaleBitmap(byte[] yuvData, int dataWidth, int dataHeight, PlanarYUVLuminanceSource source) {
        Rect rect = activity.getCropRect();
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        byte[] yuv = yuvData;
        int inputOffset = rect.top * dataWidth + rect.left;

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuv[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += dataWidth;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

}
