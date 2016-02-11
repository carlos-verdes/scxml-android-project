package com.nosolojava.android.watchmycam;

import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import com.nosolojava.android.fsm.view.impl.BasicFSMActivity;
import com.nosolojava.android.watchmycam.service.MainFSMService;
import com.nosolojava.fsm.runtime.ContextInstance;
import com.nosolojava.fsm.runtime.Event;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by cverdes on 04/01/2016.
 */
public class MainActivity extends BasicFSMActivity implements TextureView.SurfaceTextureListener {

    private static final Uri FSM_URI = Uri
            .parse("android.resource://com.nosolojava.android.watchmycam/raw/fsm#wathMyCamSession");


    public MainActivity() {
        super(MainFSMService.class, R.layout.loading_layout, FSM_URI);
    }

    private TextureView textureView = null;
    private final AtomicBoolean firstPreview = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.associateStateView(R.layout.loading_layout, null, "camera-loading-state");
        this.associateStateView(R.layout.camera_on_layout, new Runnable() {
                    @Override
                    public void run() {

                        textureView = (TextureView) findViewById(R.id.cameraTextureView);
                        textureView.setSurfaceTextureListener(MainActivity.this);

                    }
                }
                , "camera-service-ready-state", "camera-opening-state", "camera-opened-state", "view.pause");


        this.associateStateView(R.layout.camera_closed_layout, null, "camera-closed-state");
        this.associateStateView(R.layout.off_layout, null, "off-state");


    }

    @Override
    protected void onPause() {
        super.onPause();

        //release camera
        pushEventToFSM("view.pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //release camera
        pushEventToFSM("view.exit");

    }

    @Override
    protected void onResume() {
        super.onResume();

        pushEventToFSM("view.resume");

    }

    @Override
    public void onNewEvent(String eventName) {

    }


    @Override
    public void onNewEvent(String eventName, Object payload) {

        if ("controller.newImage".equals(eventName)) {
            Map<String, Object> data = (Map<String, Object>) payload;

            new AsyncTask<Object, Object, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Object[] params) {
                    Map<String, Object> data = (Map<String, Object>) params[0];

                    byte[] imageBytes = (byte[]) data.get("lastImage");
                    int counter = (int) data.get("imageCounter");
                    //logI("Draw image aync" + counter);


                    if (imageBytes != null) {
                        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        return image;

                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Bitmap image) {
                    super.onPostExecute(image);
                    if (image != null) {
                        //logI("post execute");
                        Canvas canvas = textureView.lockCanvas();
                        if (canvas != null) {
                            canvas.drawBitmap(image, 0, 0, new Paint());
                            textureView.unlockCanvasAndPost(canvas);
                        } else {

                        }
                    }

                }
            }.execute(data);


        }
    }

    @Override
    public void onNewStateConfig(ContextInstance contextInstance) {
        super.onNewStateConfig(contextInstance);

//        if (contextInstance.isStateActive("camera-opened-state") && !contextInstance.isStateActive("camera-preview-idle-state")) {
//            byte[] imageBytes = contextInstance.getDataByName("lastImage");
//            if (imageBytes != null) {
//                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//
//
//                Canvas canvas = textureView.lockCanvas();
//                canvas.drawBitmap(image, 0, 0, new Paint());
//                textureView.unlockCanvasAndPost(canvas);
//
//            }
//        }

//        if (contextInstance.isStateActive("camera-opened-state") && !firstPreview.get()) {
//            firstPreview.getAndSet(true);
//            pushEventToFSM("view.takePicture");
//        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        pushEventToFSM("view.surface.available");
//        firstPreview.getAndSet(true);
//        pushEventToFSM("view.takePicture");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        pushEventToFSM("view.surface.sizeChanged");

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        pushEventToFSM("view.surface.destroyed");

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        pushEventToFSM("view.surface.updated");


    }

    public void logI(String message) {
        Log.i("WMC-ACTIVITY", message);
    }
}
