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

import com.google.api.services.cloudadventure.model.Pickup;
import com.google.api.services.cloudadventure.model.Tile;
import com.google.cloud.solutions.cloudadventure.widget.CustomArrayAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * This Fragment belongs to GameActivity and provides controls for user actions in the game.
 *
 */
public class PlayerActionsFragment extends Fragment {

  private List<Object> currentObjectsOnTile;

  /*
   * View components.
   */
  private Button consumeButton;
  private Button examineButton;
  private Button fightButton;
  private Button talkToButton;
  private Button takeButton;
  private Button saveButton;

  /*
   * Callback to GameActivity
   */
  private OnGameActionListener callback;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Set the callback activity to use
    try {
      callback = (OnGameActionListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement OnGameActionClickListener");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_player_actions, container, false);

    currentObjectsOnTile = new ArrayList<Object>();

    // Set views and register the listeners
    instantiateExamineButton(v, R.id.examine_button);
    instantiateConsumeButton(v, R.id.consume_button);
    instantiateFightButton(v, R.id.fight_button);
    instantiateTalkButton(v, R.id.talk_to_button);
    instantiateTakeButton(v, R.id.take_button);
    instantiateSaveButton(v, R.id.save_button);

    return v;
  }

  /**
   * A listener interface for GameActivity to use to process the actions this player makes.
   *
   */
  public interface OnGameActionListener {
    public List<Pickup> getPlayerPickups();
    public void consume(Pickup consumee);
    public void examine(Object examinee);
    public void fight(Object attackee, Pickup weapon);
    public void talkTo(Object talkee);
    public void take(Object takee);
    public void save();
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * This should be called each time the {@link Tile} that the {@link Player} is standing on is
   * updated, in order to ensure that the information and views are properly updated.
   */
  public void setCurrentTile(Tile tile) {
    currentObjectsOnTile = new ArrayList<Object>();
    if (tile.getCreatures() != null && !tile.getCreatures().isEmpty()) {
      currentObjectsOnTile.addAll(tile.getCreatures());
    }
    if (tile.getPickups() != null && !tile.getPickups().isEmpty()) {
      currentObjectsOnTile.addAll(tile.getPickups());
    }
    if (!currentObjectsOnTile.isEmpty()) {
      fightButton.setVisibility(View.VISIBLE);
      talkToButton.setVisibility(View.VISIBLE);
      takeButton.setVisibility(View.VISIBLE);
    } else {
      fightButton.setVisibility(View.INVISIBLE);
      talkToButton.setVisibility(View.INVISIBLE);
      takeButton.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * This should be called when a game save has been initiated.
   */
  public void startGameSave() {
    saveButton.setEnabled(false);
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * This should be called when a game save has finished.
   */
  public void endGameSave() {
    saveButton.setEnabled(true);
  }

  public List<Pickup> getPlayerPickups() {
    return callback.getPlayerPickups();
  }

  private void instantiateExamineButton(View v, int buttonId) {
    examineButton = (Button) v.findViewById(R.id.examine_button);
    examineButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        final List<Object> allAvailableObjects = new ArrayList<Object>(currentObjectsOnTile);
        allAvailableObjects.addAll(getPlayerPickups());
        CustomArrayAdapter<Object> adapter = new CustomArrayAdapter<Object>(getActivity(),
            android.R.layout.simple_list_item_1, allAvailableObjects);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.examine_dialog_title).setAdapter(adapter,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                callback.examine(allAvailableObjects.get(which));
              }
            });
        builder.create().show();
      }
    });
  }

  private void instantiateConsumeButton(View v, int buttonId) {
    consumeButton = (Button) v.findViewById(R.id.consume_button);
    consumeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        final List<Pickup> pickups = getPlayerPickups();
        CustomArrayAdapter<Pickup> adapter = new CustomArrayAdapter<Pickup>(getActivity(),
            android.R.layout.simple_list_item_1, pickups);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.consume_dialog_title).setAdapter(adapter,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                callback.consume((Pickup) pickups.get(which));
              }
            });
        builder.create().show();
      }
    });
  }

  private void instantiateFightButton(View v, int buttonId) {
    fightButton = (Button) v.findViewById(R.id.fight_button);
    fightButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CustomArrayAdapter<Object> adapter = new CustomArrayAdapter<Object>(getActivity(),
            android.R.layout.simple_list_item_1, currentObjectsOnTile);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.fight_dialog_title).setAdapter(adapter,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                final Object attackee = currentObjectsOnTile.get(which);
                final List<Pickup> pickups = getPlayerPickups();
                CustomArrayAdapter<Pickup> adapter = new CustomArrayAdapter<Pickup>(getActivity(),
                    android.R.layout.simple_list_item_1, pickups);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.weapon_dialog_title).setAdapter(adapter,
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int which) {
                        callback.fight(attackee, pickups.get(which));
                      }
                    });
                builder.create().show();
              }
            });
        builder.create().show();
      }
    });
  }

  private void instantiateTalkButton(View v, int buttonId) {
    talkToButton = (Button) v.findViewById(R.id.talk_to_button);
    talkToButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CustomArrayAdapter<Object> adapter = new CustomArrayAdapter<Object>(getActivity(),
            android.R.layout.simple_list_item_1, currentObjectsOnTile);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.talk_dialog_title).setAdapter(adapter,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                callback.talkTo(currentObjectsOnTile.get(which));
              }
            });
        builder.create().show();
      }
    });
  }

  private void instantiateTakeButton(View v, int buttonId) {
    takeButton = (Button) v.findViewById(R.id.take_button);
    takeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        CustomArrayAdapter<Object> adapter = new CustomArrayAdapter<Object>(getActivity(),
            android.R.layout.simple_list_item_1, currentObjectsOnTile);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.take_dialog_title).setAdapter(adapter,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                callback.take(currentObjectsOnTile.get(which));
              }
            });
        builder.create().show();
      }
    });
  }

  private void instantiateSaveButton(View v, int buttonId) {
    saveButton = (Button) v.findViewById(R.id.save_button);
    saveButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.save();
      }
    });
  }
}
