package com.nosolojava.android.fsm.view.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import com.nosolojava.android.fsm.handlers.impl.ViewStateLoaderHandler;
import com.nosolojava.android.fsm.io.FSM_ACTIONS;
import com.nosolojava.android.fsm.io.FSM_EXTRAS;
import com.nosolojava.android.fsm.view.FSMActivityIntegration;
import com.nosolojava.fsm.runtime.ContextInstance;

public class FSMIntentReceiver extends BroadcastReceiver {

    private final ViewStateLoaderHandler viewStateLoaderHandler;
    private final FSMActivityIntegration fsmActivity;

    public FSMIntentReceiver(FSMActivityIntegration fsmActivity, ViewStateLoaderHandler viewStateLoaderHandler) {
        super();
        this.fsmActivity = fsmActivity;
        this.viewStateLoaderHandler = viewStateLoaderHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action;
        if (intent != null && (action = intent.getAction()) != null && action.equals(FSM_ACTIONS.FSM_TO_ACTIVITY_EVENT.toString())) {
            Bundle extras = intent.getExtras();

            String eventName = extras.getString(FSM_EXTRAS.NAME.toString());

            if (extras.containsKey(FSM_EXTRAS.CONTENT.toString())) {
                Object payload = extras.get(FSM_EXTRAS.CONTENT.toString());
                this.fsmActivity.onNewEvent(eventName, payload);
            } else {
                this.fsmActivity.onNewEvent(eventName);
            }
        } else {
            ContextInstance newConfig = (ContextInstance) intent.getSerializableExtra(FSM_EXTRAS.CONTENT.toString());
            if (newConfig != null) {
                this.fsmActivity.onNewStateConfig(newConfig);
            }

            this.viewStateLoaderHandler.onActionIntent(intent);

        }


    }

}
