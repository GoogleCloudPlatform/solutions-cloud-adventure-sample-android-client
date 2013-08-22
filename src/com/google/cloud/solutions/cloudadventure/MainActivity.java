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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

/**
 * This Activity is "Home" / "Main Menu". The launcher Activity for this app.
 *
 */
public class MainActivity extends Activity {

  private String mCurrentUserHandle;

  /*
   * View components.
   */
  private ProgressDialog mProgressDialog;
  private TextView mUserHandleView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("MainActivity State", "onCreate");

    // Set the components for this Activity
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_main);
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setCanceledOnTouchOutside(false);
    mUserHandleView = (TextView) findViewById(R.id.user_handle_welcome_text);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d("MainActivity State", "onStart");
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    Log.d("MainActivity State", "onPostCreate");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d("MainActivity State", "onResume");

    // handle user logic here
    handleUserSelectionLogic();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d("MainActivity State", "onPause");
  }

  @Override
  protected void onSaveInstanceState(Bundle instanceState) {
    super.onSaveInstanceState(instanceState);
    Log.d("MainActivity State", "onSaveInstanceState");
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d("MainActivity State", "onStop");
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    Log.d("MainActivity State", "onRestart");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d("MainActivity State", "onDestroy");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d("MainActivity State", "onActivityResult");
    if (resultCode != Activity.RESULT_OK) {
      return;
    }
    if (requestCode == Constants.ACCOUNT_SELECTION_ACTIVITY_REQUEST_CODE) {
      setCurrentUserHandle(data.getStringExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY));
    }
  }

  /**
   * Handles updating the current user to the most current state. If no current user is remembered
   * by the application, then the user may select an account to use from the phone.
   */
  private void handleUserSelectionLogic() {
    // Get what the current user handle should be
    SharedPreferences settings =
        getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    String userHandleFromSettings;
    if (settings != null) {
      userHandleFromSettings = settings.getString(Constants.USER_HANDLE_SHARED_PREFS_KEY, null);
      if (userHandleFromSettings != null) {
        setCurrentUserHandle(userHandleFromSettings);
      } else {
        selectAccount();
      }
    }
  }

  /**
   * Sets up the current user and performs a few data checks for user set up.
   */
  private void setCurrentUserHandle(String handle) {
    mCurrentUserHandle = handle;
    mUserHandleView.setText(getString(R.string.hello_prefix) + mCurrentUserHandle);
    writeHandleToSharedPrefs(mCurrentUserHandle);
    GCMIntentService.register(MainActivity.this, mCurrentUserHandle);
  }

  /**
   * Writes the user handle to SharedPreferences.
   */
  private void writeHandleToSharedPrefs(String handle) {
    SharedPreferences settings = getSharedPreferences(Constants.SHARED_PREFS_NAME,
        Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(Constants.USER_HANDLE_SHARED_PREFS_KEY, handle);
    editor.commit();
  }

  /**
   * Starts the AccountSelectionActivity, where users can switch to a different account and/or
   * choose a unique username for a new account to use with this application.
   */
  private void selectAccount() {
    Intent intent = new Intent(this, AccountSelectionActivity.class);
    startActivityForResult(intent, Constants.ACCOUNT_SELECTION_ACTIVITY_REQUEST_CODE);
  }

  /*
   * Home menu button-triggered methods.
   */

  public void viewHowTo(View view) {
    Intent intent = new Intent(this, HowToPlayActivity.class);
    startActivity(intent);
  }

  public void customizeGame(View view) {
    Intent intent = new Intent(this, CustomizeGameActivity.class);
    intent.putExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY, mCurrentUserHandle);
    startActivity(intent);
  }

  public void viewStats(View view) {
    Intent intent = new Intent(this, ProfileActivity.class);
    intent.putExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY, mCurrentUserHandle);
    startActivityForResult(intent, Constants.PROFILE_ACTIVITY_REQUEST_CODE);
  }

  public void switchUser(View view) {
    selectAccount();
  }
}
