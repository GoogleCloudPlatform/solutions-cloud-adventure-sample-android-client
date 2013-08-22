/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.google.cloud.solutions.cloudadventure;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.services.cloudadventure.Cloudadventure;
import com.google.api.services.cloudadventure.Cloudadventure.Builder;
import com.google.api.services.cloudadventure.model.DeviceInfo;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;
import com.google.cloud.solutions.cloudadventure.util.Constants;

/**
 * Receive a push message from the Cloud Messaging (GCM) service. This class should be
 * modified to include functionality specific to your application. This class must have a no-arg
 * constructor and pass the sender id to the superclass constructor.
 */
public class GCMIntentService extends GCMBaseIntentService {

  private final Cloudadventure endpoint;
  private static final String SENDER_ID = <<your project number>>;
  private static String userHandleString;

  /*
   * NOTE on the following constants: any updates to these will also need to be mirrored in
   * DevicePing.java in the corresponding App Engine backend.
   */
  public static final String GCM_PAYLOAD_PING_REASON = "GCM_PAYLOAD_PING_REASON";
  public static final String GCM_PAYLOAD_MESSAGE = "GCM_PAYLOAD_MESSAGE";
  public static final String GCM_PAYLOAD_FROM_USER_HANDLE = "GCM_PAYLOAD_FROM_USER_HANDLE";
  public static final String GCM_PAYLOAD_TO_USER_HANDLE = "GCM_PAYLOAD_TO_USER_HANDLE";
  public static final String GCM_PAYLOAD_GAME_ID = "GCM_PAYLOAD_GAME_ID";

  public static final String PING_REASON_GAME_INVITE = "PING_REASON_GAME_INVITE";
  public static final String PING_REASON_GAME_STARTED = "PING_REASON_GAME_STARTED";
  public static final String PING_REASON_GAME_DESTROYED = "PING_REASON_GAME_DESTROYED";
  public static final String PING_REASON_GAME_ENDED = "PING_REASON_GAME_ENDED";
  public static final String PING_REASON_PLAYER_JOINED = "PING_REASON_PLAYER_JOINED";
  public static final String PING_REASON_PLAYER_LEFT = "PING_REASON_PLAYER_LEFT";
  public static final String PING_REASON_PLAYER_END_STATS = "PING_REASON_PLAYER_END_STATS";
  public static final String PING_REASON_FRIEND_INVITE = "PING_REASON_FRIEND_INVITE";
  public static final String PING_REASON_FRIEND_ACCEPT = "PING_REASON_FRIEND_ACCEPT";

  public static final String BROADCAST_ON_MESSAGE_GAME_START = "on-message-start-event";
  public static final String BROADCAST_ON_MESSAGE_GAME_DESTROY = "on-message-destroy-event";
  public static final String BROADCAST_ON_MESSAGE_GAME_END = "on-message-end-event";
  public static final String BROADCAST_ON_MESSAGE_PLAYER_JOIN = "on-message-join-event";
  public static final String BROADCAST_ON_MESSAGE_PLAYER_LEAVE = "on-message-leave-event";
  public static final String BROADCAST_ON_MESSAGE_PLAYER_END_STATS = "on-message-stats-event";

  /**
   * Register the device for GCM.
   *
   * @param mContext the activity context
   * @param userHandle
   */
  public static void register(Context mContext, String userHandle) {
    userHandleString = userHandle;
    GCMRegistrar.checkDevice(mContext);
    GCMRegistrar.checkManifest(mContext);
    final String regId = GCMRegistrar.getRegistrationId(mContext);
    if (regId.equals("")) {
      GCMRegistrar.register(mContext, SENDER_ID);
    }
    new HandleDeviceRegistration().execute(userHandle, regId);
  }

  /**
   * Unregister the device from GCM.
   *
   * @param mContext the activity's context.
   */
  public static void unRegister(Context mContext, String userHandle) {
    userHandleString = userHandle;
    GCMRegistrar.checkDevice(mContext);
    GCMRegistrar.checkManifest(mContext);
    GCMRegistrar.unregister(mContext);
  }

  public GCMIntentService() {
    super(SENDER_ID);
    Builder endpointBuilder = new Cloudadventure.Builder(AndroidHttp.newCompatibleTransport(),
        new JacksonFactory(), new HttpRequestInitializer() {
          public void initialize(HttpRequest httpRequest) {
          }
        });
    endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
  }

  /**
   * Called on registration error. This is called in the context of a Service - no dialog or UI.
   *
   * @param context the Context
   * @param errorId an error message
   */
  @Override
  public void onError(Context context, String errorId) {
  }

  /**
   * Called when a cloud message has been received.
   */
  @Override
  public void onMessage(Context context, Intent intent) {
    if (PING_REASON_FRIEND_INVITE.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for friend invite.");
      Intent resultIntent = new Intent(context, ProfileActivity.class);
      resultIntent.putExtras(intent);
      generateNotification(context, resultIntent, ProfileActivity.class);
    } else if (PING_REASON_FRIEND_ACCEPT.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for friend acceptance.");
      Intent resultIntent = new Intent(context, ProfileActivity.class);
      resultIntent.putExtras(intent);
      generateNotification(context, resultIntent, ProfileActivity.class);
    } else if (PING_REASON_GAME_INVITE.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game invite.");
      Intent resultIntent = new Intent(context, GameActivity.class);
      resultIntent.putExtras(intent);
      resultIntent.putExtra(
          Constants.GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY,
          Constants.GAME_ENTRANCE_ACTION_NOTIFICATION);
      generateNotification(context, resultIntent, GameActivity.class);
    } else if (PING_REASON_GAME_STARTED.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game start.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_GAME_START);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    } else if (PING_REASON_GAME_DESTROYED.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game destroy.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_GAME_DESTROY);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    } else if (PING_REASON_GAME_ENDED.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game end.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_GAME_END);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    } else if (PING_REASON_PLAYER_JOINED.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game join.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_PLAYER_JOIN);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    } else if (PING_REASON_PLAYER_LEFT.equals(intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for game leave.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_PLAYER_LEAVE);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    } else if (PING_REASON_PLAYER_END_STATS.equals(
        intent.getStringExtra(GCM_PAYLOAD_PING_REASON))) {
      Log.i("GCMIntentService", "Received ping for player end stats.");
      Intent messageIntent = new Intent(BROADCAST_ON_MESSAGE_PLAYER_END_STATS);
      messageIntent.putExtras(intent);
      LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }
  }

  /**
   * Called when a registration token has been received.
   *
   * @param context the Context
   */
  @Override
  public void onRegistered(Context context, String regId) {
    try {
      DeviceInfo deviceInfo = new DeviceInfo()
        .setUserHandle(userHandleString)
        .setDeviceRegistrationId(regId);
      endpoint.devices().insert(deviceInfo).execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Called when the device has been manually unregistered.
   *
   * @param context the Context
   */
  @Override
  protected void onUnregistered(Context context, String regId) {
    try {
      endpoint.devices().remove(userHandleString).execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class HandleDeviceRegistration extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... ids) {
      Cloudadventure.Builder builder =
          new Cloudadventure.Builder(
              AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
      CloudEndpointUtils.updateBuilder(builder);
      Cloudadventure service = builder.build();

      try {
        DeviceInfo deviceInfo = service.devices().get(ids[0]).execute();
        if (deviceInfoIsEmpty(deviceInfo)) {
          Log.i("GCMIntentService",
              "Associating user " + ids[0] + " with this device for GCM registration.");
          deviceInfo = new DeviceInfo().setUserHandle(ids[0]).setDeviceRegistrationId(ids[1]);
          service.devices().insert(deviceInfo).execute();
        } else if (!ids[1].equals(deviceInfo.getDeviceRegistrationId())) {
          Log.i("GCMIntentService",
              "Associating user " + ids[0] + " with this device for GCM registration.");
          deviceInfo = new DeviceInfo().setUserHandle(ids[0]).setDeviceRegistrationId(ids[1]);
          service.devices().update(deviceInfo).execute();
        } else {
          Log.i("GCMIntentService",
              "User: " + ids[0] + " is already associated with this device for GCM.");
        }
      } catch (IOException e) {
        Log.d("GCMIntentService", "error: " + e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * Create a notification that will show up in the phone's notification bar.
   */
  private <T> void generateNotification(Context context, Intent intent, Class<T> clazz) {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle(
            getString(R.string.hello_prefix)
                + intent.getStringExtra(GCM_PAYLOAD_TO_USER_HANDLE) + "!")
        .setContentText(intent.getStringExtra(GCM_PAYLOAD_MESSAGE)).setAutoCancel(true);

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(clazz);
    stackBuilder.addNextIntent(intent);
    PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(0, mBuilder.build());
  }

  private static boolean deviceInfoIsEmpty(DeviceInfo deviceInfo) {
    return deviceInfo == null ||
        (deviceInfo.containsKey("error_message") && !deviceInfo.containsKey("userHandle"));
  }
}
