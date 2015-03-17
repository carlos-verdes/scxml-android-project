package com.nosolojava.android.watchmycam.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Toast;

import com.nosolojava.android.fsm.io.AndroidBroadcastIOProcessor;
import com.nosolojava.android.fsm.io.MESSAGE_DATA;

import org.apache.commons.beanutils.MethodUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Cam2Service extends Service {
    private static final String REMOTE_CAM_LOG_TAG = "remoteCam";

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Handler internalHandler = new IncomingHandler();
    final Messenger mMessenger = new Messenger(internalHandler);
    private CameraManager manager;
    private Uri sessionUri;

    private final Pattern controllerActionPattern = Pattern.compile("controller\\.action\\.(.*)$");
    private CameraDevice currentDevice = null;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            logI("handle message");


            String messageName = msg.getData().getString(MESSAGE_DATA.NAME.toString());

            logI(String.format("message name: %s", messageName));

            Matcher matcher = controllerActionPattern.matcher(messageName);
            if (matcher.matches()) {
                callMethod(msg, matcher);
            }

        }


    }

    private void callMethod(Message msg, Matcher matcher) {
        String action = matcher.replaceFirst("$1");

        Class<? extends Cam2Service> clazz = this.getClass();
        Method actionMethod = MethodUtils.getAccessibleMethod(clazz, action, Message.class);
        try {
            //getDevices(msg);
            if (actionMethod != null) {
                actionMethod.invoke(this, msg);
            }

        } catch (Exception e) {
            logE("Error calling service method", e);
            Toast.makeText(getApplicationContext(), "Camera service call error", Toast.LENGTH_SHORT).show();

            sendEventToFSM("service.camera.error", e);

        }
    }

    public void getDevices(Message msg) throws CameraAccessException {
        logI("getting devices");
        String[] devicesIds = manager.getCameraIdList();

        logI(String.format("device found:  %s", java.util.Arrays.toString(devicesIds)));

        sendEventToFSM("service.camera.getDevices.result", Arrays.asList(devicesIds));
    }

    public void openCamera(Message msg) throws CameraAccessException {
        String deviceId = msg.getData().getString(MESSAGE_DATA.CONTENT.toString());

        Handler openCameraHandler = this.internalHandler;

        CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                currentDevice = camera;

                logI(String.format("Camera opened %s", camera.getId()));
                Toast.makeText(getApplicationContext(), "Camera opened", Toast.LENGTH_SHORT).show();

                sendEventToFSM("service.camera.opened", camera.getId());
            }

            @Override
            public void onDisconnected(CameraDevice camera) {

                closeCurrentCamera();


                logI(String.format("Camera disconnected %s", camera.getId()));
                Toast.makeText(getApplicationContext(), "Camera disconnected", Toast.LENGTH_SHORT).show();

                sendEventToFSM("service.camera.disconnected", camera.getId());

            }

            @Override
            public void onError(CameraDevice camera, int error) {
                closeCurrentCamera();

                logE(String.format("Error opening camera %s, errorCode: %d", camera.getId(), error));
                Toast.makeText(getApplicationContext(), "Camera error when opening", Toast.LENGTH_SHORT).show();

                sendEventToFSM("service.camera.error", camera.getId());

            }

            @Override
            public void onClosed(CameraDevice camera) {
                super.onClosed(camera);

                currentDevice = null;

                logE(String.format("Camera closed %s", camera.getId()));
                Toast.makeText(getApplicationContext(), "Camera closed", Toast.LENGTH_SHORT).show();

                sendEventToFSM("service.camera.closed", camera.getId());

            }
        };

        this.manager.openCamera(deviceId, cameraStateCallback, openCameraHandler);

    }

    public void closeCurrentCamera(Message msg) {
        closeCurrentCamera();
    }

    public static class CustomGLSurfaceView extends GLSurfaceView  implements GLSurfaceView.Renderer{

        public CustomGLSurfaceView(Context context) {
            super(context);
            logI("gl surfaceView created");

        }


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            logI("gl surface created");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            logI("gl surface changed");

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            logI("gl draw frame");

        }
    }

    public void takePicture(Message msg) throws CameraAccessException {


        //get camera characteristics
        CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(this.currentDevice.getId());

        StreamConfigurationMap configs= cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] sizes= configs.getOutputSizes(ImageFormat.JPEG);


        final ImageReader imageReader= ImageReader.newInstance(sizes[0].getWidth(), sizes[0].getHeight(),ImageFormat.JPEG,2);
        final Surface jpegSurface = imageReader.getSurface();

        List<Surface> outputs= new ArrayList<Surface>();
        outputs.add(jpegSurface);

        currentDevice.createCaptureSession(outputs,new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                logI("on camera session configured");

                try {
                    CaptureRequest.Builder snapshotBuilder = currentDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    snapshotBuilder.addTarget(jpegSurface);

                    CaptureRequest snapshotRequest = snapshotBuilder.build();

                    session.capture(snapshotRequest,new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                        }

                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);

                            Image image = imageReader.acquireLatestImage();

                            if(image!=null){

                                File sdCardDirectory = Environment.getExternalStorageDirectory();
                                File imageFile = new File(sdCardDirectory, "test.jpg");

                                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                byte[] bytes = new byte[buffer.remaining()];
                                buffer.get(bytes);
                                FileOutputStream output = null;
                                try {
                                    output = new FileOutputStream(imageFile);
                                    output.write(bytes);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    image.close();
                                    if (null != output) {
                                        try {
                                            output.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }

                        }
                    },internalHandler);

                } catch (CameraAccessException e) {
                    logE("error creating snapshot",e);
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                logE("on camera session error");
            }
        },this.internalHandler);


    }

    private void closeCurrentCamera() {
        if (currentDevice != null) {
            currentDevice.close();
        }
        currentDevice = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        logI("creating cam service");
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

    }


    @Override
    public void onDestroy() {
        logI("destroying cam service");
        super.onDestroy();
    }


    /**
     * When binding to the service, we return an interface to our messenger for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        logI("onbind");

        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        this.sessionUri = intent.getData();

        sendEventToFSM("service.camera.ready");

        return mMessenger.getBinder();
    }

    protected void sendEventToFSM(String event) {
        sendEventToFSM(event, null);
    }

    protected void sendEventToFSM(String event, Object data) {
        AndroidBroadcastIOProcessor.sendMessageToFSM(this, MainFSMService.class, this.sessionUri, event, data);
    }


    public static void logI(String message) {
        Log.i(REMOTE_CAM_LOG_TAG, message);
    }

    public static void logE(String message) {
        Log.e(REMOTE_CAM_LOG_TAG, message);
    }

    public static void logE(String message, Throwable t) {
        Log.e(REMOTE_CAM_LOG_TAG, message, t);
    }
}
