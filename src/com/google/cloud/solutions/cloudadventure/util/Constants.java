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

package com.google.cloud.solutions.cloudadventure.util;

public class Constants {

  /*
   * SharedPreferences keys.
   */
  public static final String SHARED_PREFS_NAME = "USER_PREFS";
  public static final String USER_ACCT_SHARED_PREFS_KEY = "USER_ID";
  public static final String USER_HANDLE_SHARED_PREFS_KEY = "USER_HANDLE";

  /*
   * Intent extra keys.
   */
  public static final String USER_ACCT_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.USER_ID";
  public static final String USER_HANDLE_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.USER_HANDLE";
  public static final String OK_MESSAGE_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.OK";
  public static final String SELECTED_MAPTYPE_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.MAPTYPE";
  public static final String GAME_ID_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.GAME_ID";
  public static final String INVITED_FRIENDS_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.INVITED_FRIENDS";
  public static final String GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.GAME_ENTRANCE_ACTION";
  public static final String GAME_SCORES_GEMS_COLLECTED_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.SCORES_GEMS_COLLECTED";
  public static final String GAME_SCORES_DEATH_NUM_INTENT_EXTRA_KEY =
      "com.google.cloud.solutions.cloudadventure.SCORES_DEATH_NUM";

  /*
   * Activity request codes.
   */
  public static final int HANDLE_SELECTION_ACTIVITY_REQUEST_CODE = 1;
  public static final int GAME_ACTIVITY_REQUEST_CODE = 2;
  public static final int ACCOUNT_SELECTION_ACTIVITY_REQUEST_CODE = 3;
  public static final int PROFILE_ACTIVITY_REQUEST_CODE = 4;

  /*
   * Values for GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY.
   */
  public static final String GAME_ENTRANCE_ACTION_CREATOR = "AS_CREATOR";
  public static final String GAME_ENTRANCE_ACTION_NOTIFICATION = "BY_INVITE";
  public static final String GAME_ENTRANCE_ACTION_RESUME = "RESUME";
}
