package com.example.hj.testocr.decode;

import android.os.Handler;
import android.os.Looper;

import com.example.hj.testocr.activity.CaptureActivity;

import java.util.concurrent.CountDownLatch;

/**
 * Created by hj on 16/5/21.
 */
public class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";

    private final CaptureActivity activity;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;


    public DecodeThread(CaptureActivity activity){
        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);


    }

    public Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {

        }

        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }
}
