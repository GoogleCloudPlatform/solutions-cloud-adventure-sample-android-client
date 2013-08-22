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

package com.google.cloud.solutions.cloudadventure.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This is a DialogFragment class to be used when a simple "OK" button is needed.
 *
 */
public class OkDialogFragment extends DialogFragment {
  private int titleId;
  private String message;

  public interface OkDialogListener {
    public void onAck(DialogFragment dialog);
  }

  OkDialogListener mListener;

  public void setArguments(int titleId, String message) {
    this.titleId = titleId;
    this.message = message;
  }

  @Override
  public void onAttach(Activity activity) {
    Log.d("OkDialogFragment State", "onAttach");
    super.onAttach(activity);
    try {
      mListener = (OkDialogListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement OkDialogListener");
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("OkDialogFragment State", "onCreate");
    super.onCreate(savedInstanceState);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Log.d("OkDialogFragment State", "onCreateDialog");
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(titleId).setMessage(message)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            mListener.onAck(OkDialogFragment.this);
          }
        });
    return builder.create();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("OkDialogFragment State", "onCreateView");
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.d("OkDialogFragment State", "onActivityCreated");
    super.onActivityCreated(savedInstanceState);
  }
}
