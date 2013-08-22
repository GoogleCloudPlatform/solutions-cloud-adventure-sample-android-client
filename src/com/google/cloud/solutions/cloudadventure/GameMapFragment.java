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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This Fragment belongs to GameActivity. It displays the game world. The map is text-only, taking
 * after the classic idea of console-based text games.
 *
 */
public class GameMapFragment extends Fragment {

  /*
   * View components.
   */
  private ScrollView mScroller;
  private TextView mConsole;

  private StringBuilder mConsoleHistory;
  private String mCurrentCommand;

  private static final String INPUT_CHAR = "> ";
  private static final String DOUBLE_NEWLINE = "\n\n";

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_game_map, container, false);
    mScroller = (ScrollView) v.findViewById(R.id.scroller);
    mConsole = (TextView) v.findViewById(R.id.game_terminal);

    mConsoleHistory = new StringBuilder(getString(R.string.start_action));
    mConsoleHistory.append(DOUBLE_NEWLINE);
    mConsoleHistory.append(INPUT_CHAR);
    updateConsoleHistory();

    return v;
  }

  /**
   * Updates the text history of the console TextView to the current {@code mConsoleHistory} and
   * sets the ScrollView.
   */
  private void updateConsoleHistory() {
    mConsole.setText(mConsoleHistory.toString());
    mScroller.post(new Runnable() {
      @Override
      public void run() {
        mScroller.fullScroll(ScrollView.FOCUS_DOWN);
      }
    });
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * Updates the current command entered in the "console".
   */
  public void updateCommand(String command) {
    mCurrentCommand = command;
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * Updates the console view with the current command and the expected result of the command.
   * Resets the current command to emtpy string after view update.
   */
  public void enterCurrentCommandWithResult(String commandResult) {
    mConsoleHistory.append(mCurrentCommand);
    mConsoleHistory.append(DOUBLE_NEWLINE);
    mConsoleHistory.append(commandResult);
    mConsoleHistory.append(DOUBLE_NEWLINE);
    mConsoleHistory.append(INPUT_CHAR);
    updateConsoleHistory();
    mCurrentCommand = "";
  }

  /**
   * Used by GameActivity to update the state of this Fragment.
   * <p>
   * Directly appends to the console history.
   */
  public void appendToConsoleHistory(String newString) {
    mConsoleHistory.append(newString);
    updateConsoleHistory();
  }
}
