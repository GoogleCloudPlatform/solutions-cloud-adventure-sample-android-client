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
import com.google.api.services.cloudadventure.model.Game;
import com.google.api.services.cloudadventure.model.GameMessage;
import com.google.api.services.cloudadventure.model.GameUser;
import com.google.api.services.cloudadventure.model.Player;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;
import com.google.cloud.solutions.cloudadventure.util.Constants;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment.OkDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Activity allows the user to choose the settings for their new game, including the number of
 * players and the type of map.
 *
 */
public class CustomizeGameActivity extends Activity implements OkDialogListener {

  /*
   * Endpoint service.
   */
  private Cloudadventure mService;

  private String mCurrentUserHandle;
  private GameUser mCurrentUser;
  private ArrayList<String> mInvitedFriends;

  /*
   * View components.
   */
  private ListView mFriendsListView;
  private Spinner mSpinner;
  private ProgressDialog mProgressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("CustomizeGameActiviy State", "onCreate");

    // Set the components for this Activity
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_customize_game);
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setCanceledOnTouchOutside(false);

    mSpinner = (Spinner) findViewById(R.id.mazetypes_spinner);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this, R.array.mazetypes, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(adapter);

    // Build the endpoint service
    Cloudadventure.Builder builder =
        new Cloudadventure.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
    CloudEndpointUtils.updateBuilder(builder);
    mService = builder.build();

    // Handle user logic
    mCurrentUserHandle = getIntent().getStringExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY);

    new SetUserFromHandle().execute(mCurrentUserHandle);
  }

  private void finishSettingView() {
    List<String> friendsList = mCurrentUser.getFriends();
    mFriendsListView = (ListView) findViewById(R.id.friends_list_select);
    mFriendsListView.setAdapter(
        new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_multiple_choice, friendsList));

    new CheckForGameStatus().execute(mCurrentUserHandle);
  }

  private void startGameActivity(String gameId) {
    Intent intent = new Intent(this, GameActivity.class);
    intent.putExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY, mCurrentUserHandle);
    intent.putExtra(Constants.GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY,
        Constants.GAME_ENTRANCE_ACTION_CREATOR);
    intent.putExtra(Constants.GAME_ID_INTENT_EXTRA_KEY, gameId);
    intent.putExtra(Constants.SELECTED_MAPTYPE_INTENT_EXTRA_KEY, mSpinner.getSelectedItem()
        .toString());
    startActivity(intent);
  }

  /*
   * Button-triggered methods.
   */

  public void createGame(View view) {
    SparseBooleanArray checkedPositions = mFriendsListView.getCheckedItemPositions();
    mInvitedFriends = new ArrayList<String>();
    for (int i = 0; i < checkedPositions.size(); i++) {
      if (checkedPositions.valueAt(i)) {
        mInvitedFriends.add(
            mFriendsListView.getItemAtPosition(checkedPositions.keyAt(i)).toString());
      }
    }
    String selectedMazeType = mSpinner.getSelectedItem().toString();
    Log.i("CustomizeGameActivity", "Selected maze: " + selectedMazeType);

    new CreateGame().execute(selectedMazeType);
  }

  @Override
  public void onAck(DialogFragment dialog) {
    finish();
  }

  /*
   * AsyncTasks.
   */

  private class CheckForGameStatus extends AsyncTask<String, Void, Player> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mProgressDialog.show();
    }

    @Override
    protected Player doInBackground(String... handles) {
      Player player = null;
      try {
        player = mService.players().checkGame(handles[0]).execute();
      } catch (IOException e) {
        Log.e("ProfileActivity", "CheckForGameStatus error: " + e.getMessage(), e);
        mException = true;
      }

      return player;
    }

    @Override
    protected void onPostExecute(Player player) {
      mProgressDialog.dismiss();
      if (!mException  // no exception was thrown
          && player != null
          && !player.containsKey("error_message")) {  // endpoint return value was not null
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeGameActivity.this);
        builder
            .setTitle(R.string.continue_game_dialog_title)
            .setMessage("You are still in game. Would you like to try to rejoin the game "
                + "at your last save point or abandon it?")
            .setPositiveButton(R.string.dialog_rejoin_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(CustomizeGameActivity.this, GameActivity.class);
                    intent.putExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY, mCurrentUserHandle);
                    intent.putExtra(Constants.GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY,
                        Constants.GAME_ENTRANCE_ACTION_RESUME);
                    startActivity(intent);
                  }
                })
            .setNegativeButton(R.string.dialog_abandon_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    new RemovePlayerAndCleanUp().execute(mCurrentUserHandle);
                  }
                });
        builder.create().show();
      }
    }
  }

  private class RemovePlayerAndCleanUp extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... handles) {
      try {
        mService.players().remove(handles[0]).execute();
      } catch (IOException e) {
        Log.e("ProfileActivity", "RemovePlayerAndCleanUp error: " + e.getMessage(), e);
      }
      return null;
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
        Log.e("CustomizeGameActivity", "SetUserFromHandle error: " + e.getMessage(), e);
        mException = true;
      }

      return user;
    }

    @Override
    protected void onPostExecute(GameUser user) {
      mProgressDialog.dismiss();
      if (!mException  // no exception thrown
          && user != null
          && !user.containsKey("error_message")) {  // endpoint return value was not null
        if (user.getFriends() == null) {
          user.setFriends(new ArrayList<String>());
        }
        mCurrentUser = user;
        finishSettingView();
      } else {
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.cannot_haz_dialog,
            "Oops! Could not retrieve your friends list, please try again.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class CreateGame extends AsyncTask<String, Void, Game> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mProgressDialog.show();
    }

    @Override
    protected Game doInBackground(String... ids) {
      Game game = null;
      try {
        game = mService.games().create(ids[0]).execute();
        Log.i("CustomizeGameActivity", "Created game: " + game);
      } catch (IOException e) {
        Log.e("CustomizeGameActivity", "CreateGame error: " + e.getMessage(), e);
        mException = true;
      }

      return game;
    }

    @Override
    protected void onPostExecute(Game game) {
      mProgressDialog.dismiss();
      if (!mException  // no exception thrown
          && game != null
          && !game.containsKey("error_message")) {  // endpoint return value was not null
        String gameId = game.getId();
        startGameActivity(gameId);
        GameMessage msg = new GameMessage().setFrom(mCurrentUserHandle).setTo(mInvitedFriends)
            .setGameId(gameId);
        new SendInvites().execute(msg);
      } else {
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.create_game_fail_title,
            "Something went wrong with creating your game! Please try again.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class SendInvites extends AsyncTask<GameMessage, Void, Void> {
    @Override
    protected Void doInBackground(GameMessage... msg) {
      try {
        mService.games().invite(msg[0]).execute();
        Log.i("CustomizeGameActivity",
            "Sent game invite for game " + msg[0].getGameId() + " to " + msg[0].getTo() + ".");
      } catch (IOException e) {
        Log.d("CustomizeGameActivity", "SendInvites error: " + e.getMessage());
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void none) {
      finish();
    }
  }
}
