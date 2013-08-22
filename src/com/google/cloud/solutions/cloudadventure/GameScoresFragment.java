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
import com.google.api.services.cloudadventure.model.Player;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

/**
 * This takes place post-game and shows player scores as individual games finish.
 *
 */
public class GameScoresFragment extends Fragment {

  /*
   * Endpoint service.
   */
  private Cloudadventure mService;

  private String mCurrentHandle;

  /*
   * View components.
   */
  private Button mHomeButton;
  private TableLayout mScoresTable;
  private ProgressDialog mProgressDialog;

  private static final String PLAYER_TAG_SUFFIX = "-player";
  private static final String GEMS_TAG_SUFFIX = "-gems";
  private static final String MOBS_TAG_SUFFIX = "-mobs_killed";
  private static final String DEATHS_TAG_SUFFIX = "-deaths";

  /*
   * Callback to GameActivity.
   */
  private OnGameScoresClickListener callback;

  /**
   * Receives messages for player end-game statistics.
   */
  private BroadcastReceiver mStatsMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("GameScoresActivity", "Got a player scores message via broadcast.");
      Player player = new Player();
      player.setHandle(intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_FROM_USER_HANDLE));
      player.setGemsCollected(Long.parseLong(intent.getStringExtra("gems")));
      player.setMobsKilled(Long.parseLong(intent.getStringExtra("mobs_killed")));
      player.setNumDeaths(Long.parseLong(intent.getStringExtra("deaths")));
      addPlayerScore(player);
      mProgressDialog.dismiss();
    }
  };

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Set the callback activity to use
    try {
      callback = (OnGameScoresClickListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement OnGameScoresClickListener");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("GameScoresFragment State", "onCreateView");

    // Set component views
    final View v = inflater.inflate(R.layout.fragment_game_scores, container, false);
    mProgressDialog = new ProgressDialog(getActivity());
    mProgressDialog.setCanceledOnTouchOutside(false);
    mScoresTable = (TableLayout) v.findViewById(R.id.player_end_scores_table);
    mHomeButton = (Button) v.findViewById(R.id.return_home_button);
    mHomeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        returnHome();
      }
    });

    // Register the broadcast receivers from GCMIntentService
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mStatsMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_PLAYER_END_STATS));

    // Build the endpoint service
    Cloudadventure.Builder builder =
        new Cloudadventure.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
    CloudEndpointUtils.updateBuilder(builder);
    mService = builder.build();

    mProgressDialog.show();

    return v;
  }

  @Override
  public void onPause() {
    new RemovePlayerAndCleanUp().execute(mCurrentHandle);
    super.onPause();
  }

  @Override
  public void onDestroy() {
    Log.d("GameScoresFragment State", "onDestroy");
    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mStatsMsgReceiver);
    super.onDestroy();
  }

  /**
   * A listener interface meant for GameActivity, process the action this player makes.
   *
   */
  public interface OnGameScoresClickListener {
    public void returnHome();
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   */
  public void setCurrentUserHandle(String handle) {
    this.mCurrentHandle = handle;
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * This should be called each time the {@link Tile} that the {@link Player} is standing on is
   * updated, in order to ensure that the information and views are properly updated.
   */
  public void reportRetrievalIssue() {
    getView().findViewById(R.id.scores_retrieval_issue).setVisibility(View.VISIBLE);
    getView().findViewById(R.id.player_end_scores_table).setVisibility(View.INVISIBLE);
  }

  /**
   * Used by GameActivity to update the state of this Fragment. The list of players is taken from
   * all players that were currently listed in the Datastore as being in the game at end game.
   * Only update the player if the player hasn't been recorded in this table yet; if the player
   * is already in the table, it means a ping has been received containing the most updated info.
   * <p>
   * Adds the scores of a list of players.
   */
  public void addPlayersScores(List<Player> players) {
    for (Player player : players) {
      if (getView().findViewWithTag(player.getHandle()) == null) {
        updateScoresTable(player);
      }
    }
  }

  /**
   * Adds the scores of this player. This method is called through a ping from a receiver.
   * The ping is always the most updated information, so always update.
   */
  private void addPlayerScore(Player player) {
    String handle = player.getHandle();
    if (getView().findViewWithTag(handle) == null) {
      updateScoresTable(player);
    } else {
      TextView text = (TextView) getView().findViewWithTag(handle + GEMS_TAG_SUFFIX);
      text.setText(Long.toString(player.getGemsCollected()));
      text = (TextView) getView().findViewWithTag(handle + MOBS_TAG_SUFFIX);
      text.setText(Long.toString(player.getMobsKilled()));
      text = (TextView) getView().findViewWithTag(handle + DEATHS_TAG_SUFFIX);
      text.setText(Long.toString(player.getNumDeaths()));
    }
  }

  /**
   * Updates the View for showing the player scores.
   *
   * @param player the player whose scores have been updated
   */
  private void updateScoresTable(Player player) {
    String handle = player.getHandle();
    TableRow row = new TableRow(getActivity());
    row.setTag(handle);
    TextView text = new TextView(getActivity());
    text.setTag(handle + PLAYER_TAG_SUFFIX);
    text.setGravity(Gravity.CENTER);
    text.setText(handle);
    row.addView(text);
    text = new TextView(getActivity());
    text.setTag(handle + GEMS_TAG_SUFFIX);
    text.setGravity(Gravity.CENTER);
    text.setText(Long.toString(player.getGemsCollected()));
    row.addView(text);
    text = new TextView(getActivity());
    text.setTag(handle + MOBS_TAG_SUFFIX);
    text.setGravity(Gravity.CENTER);
    text.setText(Long.toString(player.getMobsKilled()));
    row.addView(text);
    text = new TextView(getActivity());
    text.setTag(handle + DEATHS_TAG_SUFFIX);
    text.setGravity(Gravity.CENTER);
    text.setText(Long.toString(player.getNumDeaths()));
    row.addView(text);
    mScoresTable.addView(row);
  }

  /**
   * Returns the user to the main screen.
   */
  private void returnHome() {
    callback.returnHome();
  }

  /*
   * AsyncTasks.
   */

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
}
