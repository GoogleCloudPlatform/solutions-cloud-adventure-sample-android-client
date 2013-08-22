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

import com.google.api.services.cloudadventure.model.Creature;
import com.google.api.services.cloudadventure.model.Pickup;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * This ArrayAdapter uses Creatures or Pickups as items, and displays the name property.
 * 
 * 
 * @param <T>
 */
public class CustomArrayAdapter<T> extends ArrayAdapter<T> {

  private int mFieldId = 0;
  private int mResource;
  private int mDropDownResource;
  private LayoutInflater mInflater;

  public CustomArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
    super(context, textViewResourceId, objects);
    mFieldId = 0;
    mResource = mDropDownResource = textViewResourceId;
    mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view;
    TextView text;

    if (convertView == null) {
      view = mInflater.inflate(mResource, parent, false);
    } else {
      view = convertView;
    }

    try {
      if (mFieldId == 0) {
        // If no custom field is assigned, assume the whole resource is a TextView
        text = (TextView) view;
      } else {
        // Otherwise, find the TextView field within the layout
        text = (TextView) view.findViewById(mFieldId);
      }
    } catch (ClassCastException e) {
      Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
      throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
    }

    T item = getItem(position);
    if (item instanceof CharSequence) {
      text.setText((CharSequence) item);
    } else if (item instanceof Creature) {
      text.setText(((Creature) item).getName());
    } else if (item instanceof Pickup) {
      text.setText(((Pickup) item).getName());
    } else {
      text.setText(item.toString());
    }

    return view;
  }

  @Override
  public View getDropDownView(int position, View convertView, ViewGroup parent) {
    View view;
    TextView text;

    if (convertView == null) {
      view = mInflater.inflate(mResource, parent, false);
    } else {
      view = convertView;
    }

    try {
      if (mFieldId == 0) {
        // If no custom field is assigned, assume the whole resource is a TextView
        text = (TextView) view;
      } else {
        // Otherwise, find the TextView field within the layout
        text = (TextView) view.findViewById(mFieldId);
      }
    } catch (ClassCastException e) {
      Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
      throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
    }

    T item = getItem(position);
    if (item instanceof CharSequence) {
      text.setText((CharSequence) item);
    } else if (item instanceof Creature) {
      text.setText(((Creature) item).getName());
    } else if (item instanceof Pickup) {
      text.setText(((Pickup) item).getName());
    } else {
      text.setText(item.toString());
    }

    return view;
  }
}