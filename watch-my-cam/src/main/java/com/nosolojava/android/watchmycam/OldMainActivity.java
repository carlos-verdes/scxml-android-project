package com.nosolojava.android.watchmycam;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nosolojava.android.fsm.view.impl.BasicFSMActivity;
import com.nosolojava.android.watchmycam.service.MainFSMService;
import com.nosolojava.fsm.runtime.ContextInstance;

import java.util.ArrayList;
import java.util.List;

public class OldMainActivity extends BasicFSMActivity {
    private static final String DISCONNECTED_STATE = "disconnected-state";

    private static final Uri FSM_URI = Uri
            .parse("android.resource://com.nosolojava.android.watchmycam/raw/fsm_old#remotecamSession");

    private ListView devicesListView;
    private List<String> devicesList = new ArrayList<>();
    private DevicesListAdapter devicesListAdapter;
    private TextureView textureView=null;

    public static class DevicesListAdapter<String> extends ArrayAdapter<String> {

        public DevicesListAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public DevicesListAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }
    }

    public OldMainActivity() {

        super(MainFSMService.class, R.layout.loading_layout, FSM_URI);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.associateStateView(R.layout.disconnected_layout, new Runnable() {
            @Override
            public void run() {
                initDevicesAdapter();

            }
        }, DISCONNECTED_STATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //release camera
        pushEventToFSM("view.closeCurrentCamera");
    }

    private void initDevicesAdapter() {
        if (this.devicesListView == null) {
            this.devicesListAdapter = new DevicesListAdapter(this, R.layout.device_row, R.id.deviceRowName, this.devicesList);
            this.devicesListView = (ListView) findViewById(R.id.devicesList);
            this.devicesListView.setAdapter(this.devicesListAdapter);

            this.textureView=(TextureView)findViewById(R.id.textureView);
        }
    }


    @Override
    public void onNewStateConfig(ContextInstance contextInstance) {
        super.onNewStateConfig(contextInstance);

        //if camera is ready
        if (contextInstance.isStateActive("camera-ready-state")) {
            //try to get the devices
            List<String> devices = contextInstance.getDataByName("cameraDevices");
            if (devices != null && devices.size() > 0) {
                this.devicesList.clear();
                this.devicesList.addAll(devices);
                if (devicesListAdapter != null) {
                    this.devicesListAdapter.notifyDataSetChanged();
                }
            }


        }

    }
}
