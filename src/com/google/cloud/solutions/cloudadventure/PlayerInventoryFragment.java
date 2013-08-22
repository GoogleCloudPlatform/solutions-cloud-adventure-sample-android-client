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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * This Fragment belongs to GameActivity. This shows player inventory and statistics.
 *
 */
public class PlayerInventoryFragment extends Fragment {

  /*
   * Callback to GameActivity
   */
  private OnPlayerViewListener callback;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    // Set the callback activity to use
    try {
      callback = (OnPlayerViewListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement OnPlayerViewListener");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_player_inventory, container, false);

    // Set views and register the listeners
    instantiateLookButton(v, R.id.look_button);
    instantiateViewSelfButton(v, R.id.view_self_button);
    instantiateViewInventoryButton(v, R.id.view_inventory_button);

    return v;
  }

  /**
   * A listener interface for GameActivity to use to process the view requests this player makes.
   *
   */
  public interface OnPlayerViewListener {
    public void viewSurroundings();
    public void viewSelf();
    public void viewInventory();
  }

  private void instantiateLookButton(View v, int buttonId) {
    Button button = (Button) v.findViewById(buttonId);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.viewSurroundings();
      }
    });
  }

  private void instantiateViewSelfButton(View v, int buttonId) {
    Button button = (Button) v.findViewById(buttonId);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.viewSelf();
      }
    });
  }

  private void instantiateViewInventoryButton(View v, int buttonId) {
    Button button = (Button) v.findViewById(buttonId);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.viewInventory();
      }
    });
  }
}
