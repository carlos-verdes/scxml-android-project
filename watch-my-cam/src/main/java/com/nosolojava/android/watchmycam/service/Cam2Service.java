package com.nosolojava.android.watchmycam.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.nosolojava.android.fsm.io.AndroidBroadcastIOProcessor;
import com.nosolojava.android.fsm.io.MESSAGE_DATA;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.MethodUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cam2Service extends Service {
    private static final String REMOTE_CAM_LOG_TAG = "remoteCam";

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private CameraManager manager;
    private Uri sessionUri;

    private final Pattern controllerActionPattern = Pattern.compile("controller\\.action\\.(.*)$");

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            logI("handle message");

            Toast.makeText(getApplicationContext(), "hello, a messages arrived with reflection", Toast.LENGTH_SHORT).show();

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
            sendEventToFSM("service.camera.error", e);

        }
    }

    public void getDevices(Message msg) throws CameraAccessException {
        logI("getting devices");
        String[] devicesIds = manager.getCameraIdList();

        logI(String.format("device found:  %s", java.util.Arrays.toString(devicesIds)));

        sendEventToFSM("service.camera.getDevices.result", Arrays.asList(devicesIds));
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


    private void logI(String message) {
        Log.i(REMOTE_CAM_LOG_TAG, message);
    }

    private void logE(String message, Throwable t) {
        Log.e(REMOTE_CAM_LOG_TAG, message, t);
    }
}
