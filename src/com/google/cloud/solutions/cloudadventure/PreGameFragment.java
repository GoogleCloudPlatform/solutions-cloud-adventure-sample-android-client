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

import com.google.cloud.solutions.cloudadventure.util.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * This Activity takes places before the game actually starts. Players who join the game can see who
 * else has joined, and the creator of the game can then hit the "start" button at any time. Players
 * may leave the game at any time. The creator may also cancel the game at any time.
 *
 */
public class PreGameFragment extends Fragment {

  private ArrayList<String> mCurrentHandles;
  private boolean mIsCreator;

  /*
   * View components.
   */
  private ListView mPlayersView;
  private TextView mStatusMessage;
  private Button mStartButton;
  private Button mCancelButton;
  private Button mLeaveButton;

  /*
   * Callback to GameActivity.
   */
  private OnPreGameClickListener callback;

  /**
   * Receives messages for players joining the game.
   */
  private BroadcastReceiver mJoinMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("PreGameFragment", "Got a join message via broadcast.");
      String from = intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_FROM_USER_HANDLE);
      addCurrentPlayer(from);
    }
  };

  /**
   * Receives messages for players leaving the game from this screen.
   */
  private BroadcastReceiver mLeaveMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("PreGameFragment", "Got a leave message via broadcast.");
      String from = intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_FROM_USER_HANDLE);
      removeCurrentPlayer(from);
    }
  };

  /**
   * Receives messages for the creator cancelling the game.
   */
  private BroadcastReceiver mDestroyMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("PreGameFragment", "Got a destroy message via broadcast.");
      showGameDestroyDialog(intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_MESSAGE));
    }
  };

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Set the callback activity to use
    try {
      callback = (OnPreGameClickListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement OnPreGameClickListener");
    }

    // Handle entrance logic
    if (Constants.GAME_ENTRANCE_ACTION_CREATOR.equals(activity.getIntent().getStringExtra(
        Constants.GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY))) {
      mIsCreator = true;
    } else {
      mIsCreator = false;
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // Set component views
    final View v = inflater.inflate(R.layout.fragment_pre_game, container, false);
    mPlayersView = (ListView) v.findViewById(R.id.game_players_list);
    mStatusMessage = (TextView) v.findViewById(R.id.game_waiting_text);

    instantiateStartButton(v, R.id.start_game_button);
    instantiateCancelButton(v, R.id.cancel_game_button);
    instantiateLeaveButton(v, R.id.leave_game_button);

    // Register the broadcast receivers from GCMIntentService
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mJoinMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_PLAYER_JOIN));
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mLeaveMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_PLAYER_LEAVE));
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDestroyMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_GAME_DESTROY));

    return v;
  }

  @Override
  public void onDestroy() {
    Log.d("PreGameFragment State", "onDestroy");
    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mJoinMsgReceiver);
    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mLeaveMsgReceiver);
    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDestroyMsgReceiver);

    super.onDestroy();
  }

  /**
   * A listener interface for GameActivity to use to process the actions this player makes.
   *
   */
  public interface OnPreGameClickListener {
    public void startGame();
    public void cancelGame();
    public void leaveGame();
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   */
  public void addCurrentPlayers(ArrayList<String> handles) {
    if (mCurrentHandles == null) {
      mCurrentHandles = new ArrayList<String>();
    }
    mCurrentHandles.removeAll(handles);
    mCurrentHandles.addAll(handles);
    updatePlayersListView();
  }

  private void showGameDestroyDialog(String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.game_abandoned_dialog_title).setMessage(message)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            getActivity().finish();
          }
        });
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private void addCurrentPlayer(String handle) {
    if (mCurrentHandles == null) {
      mCurrentHandles = new ArrayList<String>();
    }
    mCurrentHandles.remove(handle);
    mCurrentHandles.add(handle);
    updatePlayersListView();
  }

  private void removeCurrentPlayer(String handle) {
    if (mCurrentHandles != null) {
      mCurrentHandles.remove(handle);
    }
    updatePlayersListView();
  }

  private void updatePlayersListView() {
    mPlayersView.setAdapter(new ArrayAdapter<String>(getActivity(),
        android.R.layout.simple_list_item_1, mCurrentHandles));
    if (mIsCreator) {
      mStartButton.setVisibility(View.VISIBLE);
      mCancelButton.setVisibility(View.VISIBLE);
    } else {
      mLeaveButton.setVisibility(View.VISIBLE);
      mStatusMessage.setVisibility(View.VISIBLE);
    }
  }

  private void instantiateStartButton(View v, int buttonId) {
    mStartButton = (Button) v.findViewById(buttonId);
    mStartButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.startGame();
      }
    });
  }

  private void instantiateCancelButton(View v, int buttonId) {
    mCancelButton = (Button) v.findViewById(buttonId);
    mCancelButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.cancelGame();
      }
    });
  }

  private void instantiateLeaveButton(View v, int buttonId) {
    mLeaveButton = (Button) v.findViewById(buttonId);
    mLeaveButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.leaveGame();
      }
    });
  }
}
