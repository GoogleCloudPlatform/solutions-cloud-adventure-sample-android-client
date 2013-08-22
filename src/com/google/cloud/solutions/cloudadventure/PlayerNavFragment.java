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

import com.google.api.services.cloudadventure.model.Tile;
import com.google.cloud.solutions.cloudadventure.world.MapUtils.Cardinal;
import com.google.cloud.solutions.cloudadventure.world.MapUtils.DirectionMapper;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

/**
 * This Fragment belongs to GameActivity. It contains a user-interactive part of this screen,
 * allowing users to choose navigational directions.
 *
 *
 */
public class PlayerNavFragment extends Fragment {

  private DirectionMapper mDirectionMapper;

  /*
   * View components.
   */
  private Button mForwardButton;
  private Button mRightButton;
  private Button mBackButton;
  private Button mLeftButton;

  /*
   * Callback to GameActivity
   */
  private OnPlayerNavClickListener callback;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Set the callback activity to use
    try {
      callback = (OnPlayerNavClickListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement OnPlayerNavClickListener");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_player_navigation, container, false);

    // Set views and register the listeners
    instantiateGoForwardButton(v, R.id.go_forward_button);
    instantiateGoRightButton(v, R.id.go_right_button);
    instantiateGoBackButton(v, R.id.go_back_button);
    instantiateGoLeftButton(v, R.id.go_left_button);

    return v;
  }

  /**
   * A listener interface for GameActivity to use to process the navigations this player makes.
   *
   */
  public interface OnPlayerNavClickListener {
    public void move(Cardinal direction);
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * Sets a new {@link DirectionMapper} with an initial orientation.
   *
   * @param currentOrientation the cardinal direction of the initial orientation for the mapper
   */
  public void setNewDirectionMapper(final Cardinal currentOrientation) {
    mDirectionMapper = new DirectionMapper(currentOrientation);
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * Sets the current {@link Tile} and decides the navigation buttons to show based upon the
   * properties of the tile.
   * <p>
   * This should be called each time the {@link Tile} that the {@link Player} is standing on is
   * updated, in order to ensure that the information and views are properly updated.
   *
   * @param tile the new current tile
   */
  public void setCurrentTile(Tile tile) {
    List<String> openTo = tile.getOpenTo();
    mForwardButton.setEnabled(openTo.contains(mDirectionMapper.getFrontCardinal().toString()));
    mRightButton.setEnabled(openTo.contains(mDirectionMapper.getRightCardinal().toString()));
    mBackButton.setEnabled(openTo.contains(mDirectionMapper.getBehindCardinal().toString()));
    mLeftButton.setEnabled(openTo.contains(mDirectionMapper.getLeftCardinal().toString()));
  }

  /**
   * Used by GameActivityi to update the state of this fragment.
   * <p>
   * Hides all but the back button. Used when there are dangerous creatures on the tile.
   */
  public void registerCreatureDanger() {
    mForwardButton.setEnabled(false);
    mRightButton.setEnabled(false);
    mLeftButton.setEnabled(false);
  }

  private void instantiateGoForwardButton(View v, int buttonId) {
    mForwardButton = (Button) v.findViewById(buttonId);
    mForwardButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.move(mDirectionMapper.getFrontCardinal());
      }
    });
  }

  private void instantiateGoRightButton(View v, int buttonId) {
    mRightButton = (Button) v.findViewById(buttonId);
    mRightButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mDirectionMapper.turnRight();
        callback.move(mDirectionMapper.getFrontCardinal()); // right is the new forward
      }
    });
  }

  private void instantiateGoBackButton(View v, int buttonId) {
    mBackButton = (Button) v.findViewById(buttonId);
    mBackButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mDirectionMapper.turnAround();
        callback.move(mDirectionMapper.getFrontCardinal()); // back is the new forward
      }
    });
  }

  private void instantiateGoLeftButton(View v, int buttonId) {
    mLeftButton = (Button) v.findViewById(buttonId);
    mLeftButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mDirectionMapper.turnLeft();
        callback.move(mDirectionMapper.getFrontCardinal()); // left is the new forward
      }
    });
  }
}
