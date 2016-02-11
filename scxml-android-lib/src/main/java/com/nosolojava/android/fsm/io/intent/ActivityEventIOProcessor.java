package com.nosolojava.android.fsm.io.intent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.nosolojava.android.fsm.io.AbstractAndroidIOProcessor;
import com.nosolojava.android.fsm.io.AndroidBroadcastIOProcessor;
import com.nosolojava.android.fsm.io.FSM_ACTIONS;
import com.nosolojava.android.fsm.io.FSM_EXTRAS;
import com.nosolojava.android.fsm.util.AndroidUtils;
import com.nosolojava.fsm.runtime.executable.externalcomm.Message;

/**
 * Created by cverdes on 05/02/2016.
 */
public class ActivityEventIOProcessor extends AndroidBroadcastIOProcessor {

    public static String NAME="activity-event";

    public ActivityEventIOProcessor(Context androidContext, Service fsmService) {
        super(androidContext, fsmService);
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    protected Intent createIntentFromMessage(Message message) {

        //the action is the FSM_TO_ACTIVITY_EVENT
        Intent intent= new Intent(FSM_ACTIONS.FSM_TO_ACTIVITY_EVENT.toString());

        // the event will be on the extras
        intent.putExtra(FSM_EXTRAS.NAME.toString(),message.getName());

        // the body is the extra info
        Object body = message.getBody();
        AndroidUtils.addContentToIntent(intent, body);

        // add the source uri so the receiver could answer the fsm
        intent.setData(Uri.parse(message.getSource().toString()));

        return intent;
    }
}
