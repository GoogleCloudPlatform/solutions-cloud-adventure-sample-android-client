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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.cloudadventure.Cloudadventure;
import com.google.api.services.cloudadventure.model.FriendMessage;
import com.google.api.services.cloudadventure.model.GameUser;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;
import com.google.cloud.solutions.cloudadventure.util.Constants;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment.OkDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This Activity shows the lifetime statistics of the user of the application.
 *
 */
public class ProfileActivity extends Activity implements OkDialogListener {

  /*
   * Endpoint service.
   */
  private Cloudadventure mService;

  private String mCurrentUserHandle;
  private GameUser mCurrentUser;

  /*
   * View components.
   */
  private ProgressDialog mProgressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d("ProfileActivity State", "onCreate");
    super.onCreate(savedInstanceState);

    // Build the endpoint service
    Cloudadventure.Builder builder =
        new Cloudadventure.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
    CloudEndpointUtils.updateBuilder(builder);
    mService = builder.build();

    // Set the components for this Activity
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_profile);
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setCanceledOnTouchOutside(false);

    // Handle profile entrance logic
    String pingReason = getIntent().getStringExtra(GCMIntentService.GCM_PAYLOAD_PING_REASON);
    if (GCMIntentService.PING_REASON_FRIEND_INVITE.equals(pingReason)
        || GCMIntentService.PING_REASON_FRIEND_ACCEPT.equals(pingReason)) {
      // Entered this Activity through notification. Set current vars from notification intent.
      mCurrentUserHandle = getIntent().getStringExtra(GCMIntentService.GCM_PAYLOAD_TO_USER_HANDLE);
      writeHandleToSharedPrefs(mCurrentUserHandle);
    } else {
      // Entered this Activity normally, directly through home page. Set current vars from that.
      mCurrentUserHandle = getIntent().getStringExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY);
    }

    if (GCMIntentService.PING_REASON_FRIEND_INVITE.equals(pingReason)) {
      buildFriendInviteNotificationDialog(getIntent()).show();
    }

    new SetUserFromHandle().execute(mCurrentUserHandle);
    Log.d("ProfileActivity State", "onCreate done");
  }

  private AlertDialog buildFriendInviteNotificationDialog(final Intent intent) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_MESSAGE))
        .setTitle(R.string.friend_notification_dialog_title)
        .setPositiveButton(R.string.dialog_accept_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            new AcceptFriendRequest().execute(
                new FriendMessage()
                    // friend accepter
                    .setFrom(intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_TO_USER_HANDLE))
                    // friend requester
                    .setTo(intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_FROM_USER_HANDLE)));
          }
        }).setNegativeButton(
            R.string.dialog_decline_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
          }
        });
    return builder.create();
  }

  private void writeHandleToSharedPrefs(String userHandle) {
    SharedPreferences settings =
        getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(Constants.USER_HANDLE_SHARED_PREFS_KEY, userHandle);
    editor.commit();
  }

  /**
   * This is called after a request for the current {@link GameUser} has successfully returned with
   * the refreshed user.
   */
  private void onCurrentUserRefresh() {
    TextView userHandleTextView = (TextView) findViewById(R.id.user_handle_value);
    userHandleTextView.setText(mCurrentUserHandle);
    TextView totalGamesTextView = (TextView) findViewById(R.id.total_games_value);
    totalGamesTextView.setText(String.valueOf(mCurrentUser.getTotalGames()));
    TextView totalGemsTextView = (TextView) findViewById(R.id.total_gems_found_value);
    totalGemsTextView.setText(String.valueOf(mCurrentUser.getTotalGems()));
    TextView totalMobsKilledTextView = (TextView) findViewById(R.id.total_mobs_killed_value);
    totalMobsKilledTextView.setText(String.valueOf(mCurrentUser.getTotalMobsKilled()));

    ListView friendsListView = (ListView) findViewById(R.id.friends_list);
    friendsListView.setAdapter(new ArrayAdapter<String>(
        this, android.R.layout.simple_list_item_1, mCurrentUser.getFriends()));
  }

  @Override
  public void onAck(DialogFragment dialog) {}

  /*
   * Button-triggered methods.
   */

  public void sendFriendInvite(View view) {
    EditText friendView = (EditText) findViewById(R.id.add_friend_text);
    String friend = friendView.getText().toString();
    if (mCurrentUser.getFriends().contains(friend)) {
      OkDialogFragment dialog = new OkDialogFragment();
      dialog.setArguments(
          R.string.cannot_haz_dialog, "You seem to already be friends with " + friend + ".");
      dialog.show(getFragmentManager(), "OkDialogListener");
    } else {
      FriendMessage invite = new FriendMessage();
      invite.setFrom(mCurrentUserHandle);
      invite.setTo(friend);
      new SendInvites().execute(invite);
    }
    friendView.setText("");
  }

  /*
   * AsyncTasks.
   */

  private class SendInvites extends AsyncTask<FriendMessage, Void, FriendMessage> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mProgressDialog.show();
    }

    @Override
    protected FriendMessage doInBackground(FriendMessage... friendInvites) {
      FriendMessage affirmInvite = null;
      try {
        affirmInvite = mService.users().inviteFriend(friendInvites[0]).execute();
      } catch (IOException e) {
        Log.e("ProfileActivity", "SendInvitesTask error: " + e.getMessage());
        mException = true;
      }
      return affirmInvite;
    }

    @Override
    protected void onPostExecute(FriendMessage result) {
      mProgressDialog.dismiss();
      if (!mException  // no exception thrown
          && result != null
          && !result.containsKey("error_message")) {  // endpoint return value was not null
        Log.i("ProfileActivity", "Friend request succeeded.");
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.add_friend_dialog_title,
            "You will be notified when they accept.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      } else {
        Log.w("ProfileActivity", "Malformed FriendMessage or user does not exist.");
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.add_friend_fail_dialog_title,
            "Uh-oh, try again. Please check the user handle you entered.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class SetUserFromHandle extends AsyncTask<String, Void, GameUser> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mProgressDialog.show();
    }

    @Override
    protected GameUser doInBackground(String... userHandles) {
      GameUser user = null;
      try {
        user = mService.users().getByHandle(userHandles[0]).execute();
      } catch (IOException e) {
        Log.e("ProfileActivity", "SetUserFromHandle error: " + e.getMessage(), e);
        mException = true;
      }
      return user;
    }

    @Override
    protected void onPostExecute(GameUser user) {
      mProgressDialog.dismiss();
      if (!mException  // no exception was thrown
          && user != null
          && !user.containsKey("error_message")) {  // endpoint return value was not null
        if (user.getFriends() == null) {
          user.setFriends(new ArrayList<String>());
        }
        mCurrentUser = user;
        onCurrentUserRefresh();
      } else {
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.cannot_haz_dialog,
            "Oops! Could not retrieve your user information, please try again.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class AcceptFriendRequest extends AsyncTask<FriendMessage, Void, Void> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mProgressDialog.show();
    }

    @Override
    protected Void doInBackground(FriendMessage... messageBus) {
      try {
        mService.users().acceptFriend(messageBus[0]).execute();
      } catch (IOException e) {
        Log.e("ProfileActivity", "AcceptFriendRequest error: " + e.getMessage(), e);
        mException = true;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void none) {
      mProgressDialog.dismiss();
      if (!mException) {
        new SetUserFromHandle().execute(mCurrentUserHandle);
      }
    }
  }
}
