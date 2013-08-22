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
import com.google.api.services.cloudadventure.model.GameUser;
import com.google.api.services.cloudadventure.model.Handle;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;
import com.google.cloud.solutions.cloudadventure.util.Constants;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment.OkDialogListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Activity prompts the user of the application to select a google account to use.
 *
 */
public class AccountSelectionActivity extends Activity implements OkDialogListener {
  /*
   * Endpoint service.
   */
  private Cloudadventure service;

  private GameUser currentUser;
  private String currentAccount;

  /*
   * Component views.
   */
  private EditText handleEdit;
  private TextView handleView;
  private Button selectButton;
  private ProgressDialog progressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set the view for this Activity
    setContentView(R.layout.activity_account_selection);
    progressDialog = new ProgressDialog(this);
    progressDialog.setCanceledOnTouchOutside(false);
    handleEdit = (EditText) findViewById(R.id.user_handle_edittext);
    handleView = (TextView) findViewById(R.id.user_handle_view);
    selectButton = (Button) findViewById(R.id.select_user_button);
    getActionBar().setDisplayHomeAsUpEnabled(true);

    // Handle account logic
    AccountManager am = AccountManager.get(this);
    // if you have an Android version > 4.04, you can use any type account
    Account[] accounts = am.getAccountsByType("com.google");
    List<String> accountNames = new ArrayList<String>();
    for (Account account : accounts) {
      accountNames.add(account.name);
    }

    // Update the view for this Activity
    ListView accountsListView = (ListView) findViewById(R.id.accounts_list);
    accountsListView.setAdapter(
        new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_single_choice, accountNames));
    accountsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
        currentAccount = (String) adapter.getItemAtPosition(position);
        new GetUserFromAccount().execute(currentAccount);
      }
    });

    // Build the endpoint service
    Cloudadventure.Builder builder =
        new Cloudadventure.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
    CloudEndpointUtils.updateBuilder(builder);
    service = builder.build();
  }

  /**
   * If the current user already has a user handle selected to go with the account, returns the
   * user to the main screen with the selected account. If the current user is a new user without
   * a unique user handle, then try to claim the user-entered user handle.
   */
  private void finishSelection() {
    if (currentUser.getHandle() == null || currentUser.getHandle().isEmpty()) {
      new ClaimUserHandle().execute(handleEdit.getText().toString());
    } else {
      Intent intent = new Intent();
      intent.putExtra(Constants.USER_ACCT_INTENT_EXTRA_KEY, currentUser.getAccount());
      intent.putExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY, currentUser.getHandle());
      setResult(RESULT_OK, intent);
      finish();
    }
  }

  /**
   * Sets the current user for this Activity and updates the views accordingly.
   */
  private void setCurrentUser(GameUser user) {
    currentUser = user;
    selectButton.setVisibility(View.VISIBLE);
    String handle = currentUser.getHandle();
    if (handle == null || handle.isEmpty()) {
      handleEdit.setVisibility(View.VISIBLE);
      handleView.setVisibility(View.GONE);
    } else {
      handleView.setText(handle);
      handleEdit.setVisibility(View.GONE);
      handleView.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onAck(DialogFragment dialog) {}

  /*
   * Button click methods.
   */

  public void selectUser(View view) {
    boolean valid = handleEdit.getText().toString().matches("^[a-zA-Z0-9_-]*$");

    if (valid) {
      finishSelection();
    } else {
      OkDialogFragment dialog = new OkDialogFragment();
      dialog.setArguments(R.string.cannot_haz_dialog,
          "Please select an alphanumeric username. You may also use \"_\" or \"-\".");
      dialog.show(getFragmentManager(), "OkDialogListener");
    }
  }

  /*
   * AsyncTasks.
   */

  private class GetUserFromAccount extends AsyncTask<String, Void, GameUser> {
    private boolean exception = false;

    @Override
    protected void onPreExecute() {
      progressDialog.show();
    }

    @Override
    protected GameUser doInBackground(String... userAccts) {
      GameUser user = null;
      try {
        String userAcct = userAccts[0];
        user = service.users().get(userAcct).execute();
      } catch (IOException e) {
        Log.e("AccountSelectionActivity", "GetUserFromAccount error: " + e.getMessage(), e);
        exception = true;
      }

      return user;
    }

    @Override
    protected void onPostExecute(GameUser user) {
      progressDialog.dismiss();
      if (!exception) {  // no exception thrown
        if (user != null
            && !user.containsKey("error_message")) {  // endpoint return value was not null
          Log.i("AccountSelectionActivity", "Found existing user: " + user);
          setCurrentUser(user);
        } else {
          Log.i("AccountSelectionActivity", "No existing user with account: " + currentAccount);
          new CreateNewUser().execute(currentAccount);
        }
      }
    }
  }

  private class CreateNewUser extends AsyncTask<String, Void, GameUser> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      progressDialog.show();
    }

    @Override
    protected GameUser doInBackground(String... userAccts) {
      GameUser user = null;
      try {
        user = service.users().create(userAccts[0]).execute();
      } catch (IOException e) {
        Log.e("AccountSelectionActivity", "CreateNewUser error: " + e.getMessage(), e);
        mException = true;
      }

      return user;
    }

    @Override
    protected void onPostExecute(GameUser user) {
      progressDialog.dismiss();
      if (!mException) {
        Log.i("AccountSelectionActivity", "Created user account: " + currentAccount);
        setCurrentUser(user);
      }
    }
  }

  private class ClaimUserHandle extends AsyncTask<String, Void, Handle> {
    private boolean mException = false;
    private String currentHandle;

    @Override
    protected void onPreExecute() {
      progressDialog.show();
    }
    @Override
    protected Handle doInBackground(String... handles) {
      Handle handle = null;
      try {
        currentHandle = handles[0];
        handle = service.handles().claim(currentHandle).execute();
      } catch (IOException e) {
        Log.e("AccountSelectionActivity", "ClaimUserHandle error: " + e.getMessage(), e);
        mException = true;
      }
      return handle;
    }

    @Override
    protected void onPostExecute(Handle handle) {
      progressDialog.dismiss();
      if (!mException  // no exception thrown
          && handle != null
          && !handle.containsKey("error_message")) {  // endpoint return value was not null
        if (handle.getHandle().equals("!")) {
          Log.i("AccountSelectionActivity",
              "The handle " + currentHandle + " has already been taken.");
          OkDialogFragment dialog = new OkDialogFragment();
          dialog.setArguments(R.string.cannot_haz_dialog,
              "\"" + currentHandle + "\" has already been taken. Please select another handle.");
          dialog.show(getFragmentManager(), "OkDialogListener");
        } else {
          Log.i("AccountSelectionAcitivty", "Unique handle claimed: " + currentHandle);
          currentUser.setHandle(currentHandle);
          new UpdateUser().execute(currentUser);
          finishSelection();
        }
      }
    }
  }

  private class UpdateUser extends AsyncTask<GameUser, Void, Void> {
    @Override
    protected Void doInBackground(GameUser... users) {
      try {
        service.users().update(users[0]).execute();
      } catch (IOException e) {
        Log.e("AccountSelectionActivity", "UpdateUser error: " + e.getMessage(), e);
      }
      return null;
    }
  }
}
