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

import static com.google.cloud.solutions.cloudadventure.util.Constants.GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.cloudadventure.Cloudadventure;
import com.google.api.services.cloudadventure.model.Coordinates;
import com.google.api.services.cloudadventure.model.Creature;
import com.google.api.services.cloudadventure.model.Pickup;
import com.google.api.services.cloudadventure.model.Player;
import com.google.api.services.cloudadventure.model.PlayerCollection;
import com.google.api.services.cloudadventure.model.Tile;
import com.google.cloud.solutions.cloudadventure.GameScoresFragment.OnGameScoresClickListener;
import com.google.cloud.solutions.cloudadventure.PlayerActionsFragment.OnGameActionListener;
import com.google.cloud.solutions.cloudadventure.PlayerInventoryFragment.OnPlayerViewListener;
import com.google.cloud.solutions.cloudadventure.PlayerNavFragment.OnPlayerNavClickListener;
import com.google.cloud.solutions.cloudadventure.PreGameFragment.OnPreGameClickListener;
import com.google.cloud.solutions.cloudadventure.util.CloudEndpointUtils;
import com.google.cloud.solutions.cloudadventure.util.Constants;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment;
import com.google.cloud.solutions.cloudadventure.widget.OkDialogFragment.OkDialogListener;
import com.google.cloud.solutions.cloudadventure.world.MapUtils;
import com.google.cloud.solutions.cloudadventure.world.MapUtils.Cardinal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This Activity is the main game Activity of the application. Once users arrive at this Activity,
 * they are considered to be "in game" and a {@link Player} is created for them. All players will
 * see a game lobby screen immediately upon arrival in this Activity, until the creator of the game
 * starts the game.
 *
 */
public class GameActivity extends Activity implements OkDialogListener, OnPlayerNavClickListener,
    OnPreGameClickListener, OnGameActionListener, OnGameScoresClickListener, OnPlayerViewListener {

  /*
   * Endpoint service.
   */
  private Cloudadventure mService;

  private String mGameId;
  private String mHandle;
  private Player mPlayer;

  /*
   * Fragments.
   */
  private PreGameFragment mPreGameFragment;
  private PlayerNavFragment mNavFragment;
  private PlayerActionsFragment mActionsFragment;
  private GameMapFragment mMapFragment;
  private GameScoresFragment mScoresFragment;

  /*
   * View components.
   */
  private ProgressDialog progressDialog;

  /**
   * Receives messages for game start.
   */
  private BroadcastReceiver mStartGameMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("GameActivity", "Got a GO for game start message via broadcast.");
      onGameStart();
    }
  };

  /**
   * Receives messages for game end.
   */
  private BroadcastReceiver mGameEndMsgReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i("GameActivity", "Got a game has ended message via broadcast.");
      onGameEnd();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set the components for this Activity
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_game);
    mNavFragment = (PlayerNavFragment) getFragmentManager().findFragmentById(
        R.id.navigantions_fragment);
    mActionsFragment = (PlayerActionsFragment) getFragmentManager().findFragmentById(
        R.id.game_controls_fragment);
    mMapFragment = (GameMapFragment) getFragmentManager().findFragmentById(R.id.game_map_fragment);
    progressDialog = new ProgressDialog(this);
    progressDialog.setCanceledOnTouchOutside(false);

    // Register the broadcast receivers from GCMIntentService
    LocalBroadcastManager.getInstance(this).registerReceiver(mStartGameMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_GAME_START));
    LocalBroadcastManager.getInstance(this).registerReceiver(mGameEndMsgReceiver,
        new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE_GAME_END));

    // Build the endpoint service
    Cloudadventure.Builder builder = new Cloudadventure.Builder(
        AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
    CloudEndpointUtils.updateBuilder(builder);
    mService = builder.build();

    Intent intent = getIntent();

    // Handle game entrance logic
    if (Constants.GAME_ENTRANCE_ACTION_CREATOR.equals(
        intent.getStringExtra(GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY))) {
      mHandle = intent.getStringExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY);
      mGameId = intent.getStringExtra(Constants.GAME_ID_INTENT_EXTRA_KEY);
      joinGame();
    } else if (Constants.GAME_ENTRANCE_ACTION_NOTIFICATION.equals(intent
        .getStringExtra(GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY))) {
      mHandle = intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_TO_USER_HANDLE);
      mGameId = intent.getStringExtra(GCMIntentService.GCM_PAYLOAD_GAME_ID);
      joinGame();
    } else if (Constants.GAME_ENTRANCE_ACTION_RESUME.equals(intent
        .getStringExtra(GAME_ENTRANCE_ACTION_INTENT_EXTRA_KEY))) {
      mHandle = intent.getStringExtra(Constants.USER_HANDLE_INTENT_EXTRA_KEY);
      resumeGame();
    }
  }

  @Override
  protected void onDestroy() {
    Log.d("GameActivity State", "onDestroy");
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mStartGameMsgReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mGameEndMsgReceiver);
    super.onDestroy();
  }

  /**
   * Writes the user handle of the player into SharedPreferences.
   */
  private void writeHandleToSharedPrefs(String userHandle) {
    SharedPreferences settings =
        getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(Constants.USER_HANDLE_SHARED_PREFS_KEY, userHandle);
    editor.commit();
  }

  /**
   * Shows the {@link PreGameFragment} game lobby screen and creates a new {@link Player}. This
   * method is called almost immediately, within the Activity's onCreate().
   */
  private void joinGame() {
    // Show the pre-game lobby screen
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    mPreGameFragment = new PreGameFragment();
    fragmentTransaction.add(R.id.game_activity, mPreGameFragment);
    fragmentTransaction.commit();

    new JoinGame().execute(mGameId, mHandle);
  }

  /**
   * Listener-triggered from {@link PreGameFragment}.
   * <p>
   * Starts this game. The creator sends a request to the backend to update the game to running.
   */
  public void startGame() {
    new StartGame().execute(mHandle, mGameId);
    progressDialog.show();
  }

  /**
   * Listener-triggered from {@link PreGameFragment}.
   * <p>
   * Cancels the game for all players in the game. This action can only be called while still on the
   * PreGameFragment screen, by the creator of the game.
   */
  public void cancelGame() {
    new LeaveGame().execute(mGameId, mHandle);
    new CancelGame().execute(mGameId);
    finish();
  }

  /**
   * Listener-triggered from {@link PreGameFragment}.
   * <p>
   * Leaves the game for the current Player. This action can only be called while still in the
   * PreGameFragment screen, by a player who is not the creator of the game.
   */
  public void leaveGame() {
    new LeaveGame().execute(mGameId, mHandle);
    finish();
  }

  /**
   * Resumes the game for a player who rejoins the game after this GameActivity has been destroyed.
   */
  private void resumeGame() {
    new GetPlayer().execute(mHandle);
  }

  /**
   * Ends this game, called by the current Player of this instance. Sends an end-game notification
   * to all players.
   */
  private void endGame() {
    new EndGame().execute(mHandle, mGameId);
  }

  /**
   * This method is called when the Activity receives a message via {@link BroadcastRecevier} that
   * the game has started.
   */
  private void onGameStart() {
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.remove(mPreGameFragment);
    fragmentTransaction.commit();
    progressDialog.dismiss();

    // Set the starting point for fragments
    mNavFragment.setNewDirectionMapper(Cardinal.valueOf(mPlayer.getOrientation()));
    mNavFragment.setCurrentTile(mPlayer.getCurrentTile());
  }

  /**
   * This method is called when this activity is being resumed by the current player.
   */
  private void onGameResume() {
    mNavFragment.setNewDirectionMapper(Cardinal.valueOf(mPlayer.getOrientation()));
    mNavFragment.setCurrentTile(mPlayer.getCurrentTile());
  }

  /**
   * This method is called when the Activity receives a message via {@link BroadcastRecevier} that
   * the game has ended.
   */
  private void onGameEnd() {
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    mScoresFragment = new GameScoresFragment();
    mScoresFragment.setCurrentUserHandle(mHandle);
    fragmentTransaction.add(R.id.game_activity, mScoresFragment);
    fragmentTransaction.commit();

    new SaveScoresAndSend().execute(mHandle, mGameId, Long.toString(mPlayer.getGemsCollected()),
        Long.toString(mPlayer.getMobsKilled()), Long.toString(mPlayer.getNumDeaths()));
  }

  /**
   * Listener-triggered from {@link GameScoresFragment}.
   */
  public void returnHome() {
    finish();
  }

  /**
   * Updates the Views of the Fragments affected by the player moving onto a new {@link Tile}.
   */
  private void updateNavAndActionFragmentsViewsWithTile(Tile tile) {
    mNavFragment.setCurrentTile(tile);
    mActionsFragment.setCurrentTile(tile);

    List<Creature> creatures = tile.getCreatures();
    if (creatures != null && !creatures.isEmpty()) {
      boolean ohNoesAMob = false;
      for (Creature creature : creatures) {
        if (ohNoesAMob || creature.getMaxEffect() < 0) {
          ohNoesAMob = true;
        }
      }

      if (ohNoesAMob) {
        mNavFragment.registerCreatureDanger();
      }
    }
  }

  /**
   * Builds a "current surroundings" description from the information on the player's current tile.
   */
  private String getSurroundingsDescription() {
    Log.d("GameActivity", "Number gems remaining: " + mPlayer.getMaze().getGemsRemaining().size());
    for (Coordinates coord : mPlayer.getMaze().getGemsRemaining()) {
      Log.d("GameActivity", "Remaining gem location: [" + coord.getX() + ", " + coord.getY() + "]");
    }

    if (checkForGameOver()) {
      endGame();
    }

    Tile tile = mPlayer.getCurrentTile();
    List<Creature> creatures = tile.getCreatures();
    List<Pickup> pickups = tile.getPickups();

    StringBuilder descriptionBuilder = new StringBuilder(tile.getDescription());
    descriptionBuilder.append("\nYou are now facing " + mPlayer.getOrientation() + "\n\n");
    if ((creatures != null && !creatures.isEmpty()) || (pickups != null && !pickups.isEmpty())) {
      descriptionBuilder.append("Items of interest here: \n");
      boolean ohNoesAMob = false;
      if (creatures != null && !creatures.isEmpty()) {
        for (Creature creature : creatures) {
          if (ohNoesAMob || creature.getMaxEffect() < 0) {
            ohNoesAMob = true;
          }
          descriptionBuilder.append(creature.getName());
          descriptionBuilder.append("\n");
        }
      }
      if (pickups != null && !pickups.isEmpty()) {
        for (Pickup pickup : pickups) {
          descriptionBuilder.append(pickup.getName());
          descriptionBuilder.append("\n");
        }
      }
      if (ohNoesAMob) {
        descriptionBuilder
            .append("Some of the creatures are blocking many or all of the passageways out.\n");
      }
    } else {
      descriptionBuilder.append("\nYou are alone here.");
    }

    return descriptionBuilder.toString();
  }

  /**
   * Informs the player that they have died and reset them to their first location with their
   * original base items.
   */
  private void playerDiesAndRespawns() {
    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
    builder.setTitle(R.string.player_died_dialog_title)
        .setMessage("Oh no. That didn't work out so well. Let's start over, shall we?")
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Reset inventory fragment
            mPlayer.setPickups(new ArrayList<Pickup>(mPlayer.getBaseItems()));
            mPlayer.setGemsCollected(0L);
            mPlayer.setNumDeaths(mPlayer.getNumDeaths() + 1);
            mPlayer.setCurrentHP(mPlayer.getMaxHP());

            // Reset navigation and control fragments
            Coordinates startCoordinates = mPlayer.getMaze().getStartingCoordinates();
            Tile startTile = mPlayer.getMaze().getGrid().get(startCoordinates.getX())
                .get(startCoordinates.getY());
            mPlayer.setCurrentTile(startTile);
            updateNavAndActionFragmentsViewsWithTile(startTile);

            // New directionmapper
            mPlayer.setOrientation(startTile.getOpenTo().get(0));
            mNavFragment.setNewDirectionMapper(Cardinal.valueOf(mPlayer.getOrientation()));

            // Reset map fragment
            mMapFragment.appendToConsoleHistory("\n\nYou have died.\n\nYou respawn at the start, "
                + "bereft of everything except for a few small items.\n\n");
          }
        });
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  /**
   * Is this {@link Pickup} item a gem?
   */
  private boolean isGem(Pickup pickup) {
    return "gem".equalsIgnoreCase(pickup.getName());
  }
  /**
   * Checks the conditions to see if this player has completed the map objectives.
   */
  private boolean checkForGameOver() {
    return mPlayer.getMaze().getGemsRemaining().isEmpty();
  }

  /**
   * Listener-triggered from {@link OkDialogFragment}.
   */
  @Override
  public void onAck(DialogFragment dialog) {
    finish();
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public List<Pickup> getPlayerPickups() {
    return mPlayer.getPickups();
  }

  /**
   * Listener-triggered from {@link PlayerInventoryFragment}.
   */
  @Override
  public void viewSelf() {
    mMapFragment.updateCommand(getString(R.string.command_view_self));
    mMapFragment.enterCurrentCommandWithResult(String.format(getString(R.string.view_self_text),
        mPlayer.getCurrentHP(), mPlayer.getMaxHP()));
  }

  /**
   * Listener-triggered from {@link PlayerInventoryFragment}.
   */
  @Override
  public void viewInventory() {
    mMapFragment.updateCommand(getString(R.string.command_view_inventory));
    StringBuilder inventory = new StringBuilder("You are carrying:");
    for (Pickup pickup : mPlayer.getPickups()) {
      inventory.append("\n");
      inventory.append(pickup.getName());
    }
    mMapFragment.enterCurrentCommandWithResult(inventory.toString());

  }

  /**
   * Listener-triggered from {@link PlayerNavFragment}.
   * <p>
   * Finds the next tile and update the states of the map, nav, and control fragments.
   */
  @Override
  public void move(Cardinal direction) {
    Tile newTile = MapUtils.getNextTile(mPlayer, direction);
    mPlayer.setOrientation(direction.toString());
    mPlayer.setCurrentTile(newTile);
    updateNavAndActionFragmentsViewsWithTile(newTile);
    mMapFragment.updateCommand(getString(R.string.command_move) + direction.toString());
    mMapFragment.enterCurrentCommandWithResult(getSurroundingsDescription());
    Log.i("GameActivity", mPlayer.getHandle() + " is now facing " + mPlayer.getOrientation()
        + " and on Tile " + mPlayer.getCurrentTile() + ".");
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void viewSurroundings() {
    updateNavAndActionFragmentsViewsWithTile(mPlayer.getCurrentTile());
    mMapFragment.updateCommand(getString(R.string.command_look));
    mMapFragment.enterCurrentCommandWithResult(getSurroundingsDescription());
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void examine(Object examinee) {
    if (examinee instanceof Creature) {
      Creature creature = (Creature) examinee;
      mMapFragment.updateCommand(getString(R.string.command_examine) + creature.getName());
      mMapFragment.enterCurrentCommandWithResult(creature.getDescription());
    } else if (examinee instanceof Pickup) {
      Pickup pickup = (Pickup) examinee;
      mMapFragment.updateCommand(getString(R.string.command_examine) + pickup.getName());
      if (isGem(pickup)) {
        mMapFragment.enterCurrentCommandWithResult("a glittering gem. pick it up!");
      } else if (pickup.getMaxEffect() < 0) {
        mMapFragment.enterCurrentCommandWithResult(pickup.getDescription()
            + ". It looks like you can get about " + pickup.getNumUses()
            + " more battles out of it.");
      } else {
        mMapFragment.enterCurrentCommandWithResult(pickup.getDescription());
      }
    }
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void fight(Object attackee, Pickup weapon) {
    if (attackee instanceof Creature) {
      Creature creature = (Creature) attackee;
      mMapFragment.updateCommand(
          String.format(getString(R.string.command_fight), creature.getName(), weapon.getName()));
      StringBuilder mapOutput = new StringBuilder();

      long encounterableEffect = creature.getMaxEffect();
      long encounterableHp = creature.getHitPoints();
      weapon.setNumUses(weapon.getNumUses() - 1);

      if (encounterableEffect >= 0) {
        // You attack a good guy...
        mPlayer.getCurrentTile().getCreatures().remove(creature);
        updateNavAndActionFragmentsViewsWithTile(mPlayer.getCurrentTile());
        if (weapon.getMaxEffect() < 0) {
          // ...with a weapon
          mapOutput.append("Wow. You killed a " + creature.getName()
              + ". This is why you can't have nice things.");
        } else {
          // ...with something that is not a weapon
          mapOutput.append("You throw the " + weapon.getName() + " at the " + creature.getName()
              + ". It looks at you sadly, and turns and vanishes into the darkness.");
        }
      } else {
        // You attack a bad guy...
        if (weapon.getMaxEffect() < 0) {
          // ...with a weapon
          encounterableHp = encounterableHp + weapon.getMaxEffect(); // player hits first
          while (mPlayer.getCurrentHP() > 0 && encounterableHp > 0) { // battle commences
            encounterableHp = encounterableHp + weapon.getMaxEffect();
            mPlayer.setCurrentHP(mPlayer.getCurrentHP() + encounterableEffect);
          }
          if (mPlayer.getCurrentHP() > 0) {
            mPlayer.setMobsKilled(mPlayer.getMobsKilled() + 1);
            mPlayer.getCurrentTile().getCreatures().remove(creature);
            updateNavAndActionFragmentsViewsWithTile(mPlayer.getCurrentTile());
            mapOutput.append("You and the " + creature.getName()
                + " battle it out. Eventually you stand victorious!");
          } else {
            playerDiesAndRespawns();
            return;
          }
        } else {
          // ...with something that is not a weapon
          mPlayer.setCurrentHP(mPlayer.getCurrentHP() + encounterableEffect);
          if (mPlayer.getCurrentHP() > 0) {
            mPlayer.getPickups().remove(weapon);
            mapOutput.append("You throw the " + weapon.getName()
                + " at it. It is pretty useless and the " + creature.getName() + " hits you for "
                + encounterableEffect + ". Try something else.");
          } else {
            playerDiesAndRespawns();
            return;
          }
        }
      }
      if (weapon.getNumUses() > 0) {
        mapOutput
            .append("\n\nYour " + weapon.getName()
                + " is a bit more battered than before, but perhaps it was worth it. "
                + "You put it away again.");
      } else {
        mPlayer.getPickups().remove(weapon);
        mapOutput
            .append("\n\nYour " + weapon.getName()
                + " has served you faithfully until now, "
                + "but it has reached the end of its use and shatters.");
      }
      mMapFragment.enterCurrentCommandWithResult(mapOutput.toString());
    } else if (attackee instanceof Pickup) {
      Pickup pickup = (Pickup) attackee;
      mMapFragment.updateCommand(
          String.format(getString(R.string.command_fight), pickup.getName(), weapon.getName()));
      StringBuilder mapOutput =
          new StringBuilder("Don't be absurd, why would you attack the " + pickup.getName() + "?");
      weapon.setNumUses(weapon.getNumUses() - 1);
      if (weapon.getNumUses() > 0) {
        mapOutput.append("\n\nIt clearly didn't help you and it put a dent in your "
            + weapon.getName() + " anyway.");
      } else {
        mPlayer.getPickups().remove(weapon);
        mapOutput
            .append("\n\nYour " + weapon.getName()
                + " has served you faithfully until now, "
                + "but it has reached the end of its use and shatters.");
      }
      mMapFragment.enterCurrentCommandWithResult(mapOutput.toString());
    }
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void talkTo(Object talkee) {
    if (talkee instanceof Creature) {
      Creature creature = (Creature) talkee;
      mMapFragment.updateCommand(getString(R.string.command_talk) + creature.getName());

      // Set the effect that the creature has on the player
      long creatureEffect = creature.getMaxEffect();
      long effectiveCreatureEffect = Math.min(creature.getMaxEffect(),
          mPlayer.getMaxHP() - mPlayer.getCurrentHP());
      mPlayer.setCurrentHP(mPlayer.getCurrentHP() + effectiveCreatureEffect);

      if (creatureEffect < 0) {
        if (mPlayer.getCurrentHP() > 0) {
          mMapFragment.enterCurrentCommandWithResult(
              "You try to approach to the " + creature.getName() + " and it hits you for "
          + effectiveCreatureEffect + ". What did you think would happen?");
        } else {
          playerDiesAndRespawns();
          return;
        }
      } else {
        mPlayer.getCurrentTile().getCreatures().remove(creature);
        updateNavAndActionFragmentsViewsWithTile(mPlayer.getCurrentTile());
        mMapFragment.enterCurrentCommandWithResult("You approach the " + creature.getName()
            + ". It doesn't say a thing but you benefit from its healing aura by "
            + effectiveCreatureEffect + ". It smiles at you and vanishes into the darkness.");
      }
    } else if (talkee instanceof Pickup) {
      Pickup pickup = (Pickup) talkee;
      mMapFragment.updateCommand(getString(R.string.command_talk) + pickup.getName());
      mMapFragment.enterCurrentCommandWithResult("Talking to a " + pickup.getName()
          + "? Do you often hold conversations with inanimate objects?");
    }
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void take(Object takee) {
    if (takee instanceof Creature) {
      Creature creature = (Creature) takee;
      mMapFragment.updateCommand(getString(R.string.command_take) + creature.getName());

      // This was an Creature. Get the creature and process.
      long creatureEffect = creature.getMaxEffect();
      mPlayer.setCurrentHP(mPlayer.getCurrentHP() + creatureEffect);
      if (creatureEffect < 0) {
        // Player tries to pick up a "bad guy"
        if (mPlayer.getCurrentHP() > 0) {
          mMapFragment.enterCurrentCommandWithResult("The " + creature.getName()
              + " did not appreciate you trying to pick it up " + "and hits you for "
              + creatureEffect + ".");
        } else {
          playerDiesAndRespawns();
          return;
        }
      } else {
        // Player tries to pick up a "good guy"
        mMapFragment.enterCurrentCommandWithResult(
            "Why are you trying to pick up a " + creature.getName() + "? Stop.");
      }
    } else if (takee instanceof Pickup) {
      Pickup pickup = (Pickup) takee;

      mMapFragment.updateCommand(getString(R.string.command_take) + pickup.getName());

      // If we get to here, the select item is indeed a proper Pickup, and the
      // user can add it to inventory.
      mPlayer.getPickups().add(pickup);
      // Update the nav, control, and map fragments
      mPlayer.getCurrentTile().getPickups().remove(pickup);
      updateNavAndActionFragmentsViewsWithTile(mPlayer.getCurrentTile());
      if (isGem(pickup)) {
        mMapFragment
            .enterCurrentCommandWithResult("You pick up the gem and add it to your inventory. ");
        mPlayer.getMaze().getGemsRemaining().remove(mPlayer.getCurrentTile().getCoord());
        mPlayer.setGemsCollected(mPlayer.getGemsCollected() + 1);
      } else {
        mMapFragment.enterCurrentCommandWithResult(
            "You have added " + pickup.getName() + " to your inventory.");
      }
    }
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void consume(Pickup consumee) {
    mMapFragment.updateCommand(getString(R.string.command_consume) + consumee.getName());

    long pickupEffect = consumee.getMaxEffect();
    long effectivePickupEffect = Math.min(consumee.getMaxEffect(),
        mPlayer.getMaxHP() - mPlayer.getCurrentHP());
    mPlayer.setCurrentHP(mPlayer.getCurrentHP() + effectivePickupEffect);
    if (pickupEffect < 0) {
      // Player tries to consume a weapon
      if (mPlayer.getCurrentHP() > 0) {
        mMapFragment.enterCurrentCommandWithResult("You tried to consume a " + consumee.getName()
            + ". I mean, whatever floats your boat, but that just walloped you for "
            + effectivePickupEffect + ".");
      } else {
        playerDiesAndRespawns();
        return;
      }
    } else if (pickupEffect > 0) {
      // Player consumes a healing item
      mPlayer.getPickups().remove(consumee);
      mMapFragment.enterCurrentCommandWithResult(
          "Good call. That just revived you for " + effectivePickupEffect + ".");
    } else {
      // Player tries to consume something with zero effect
      mMapFragment.enterCurrentCommandWithResult(
          "You tried to consume a " + consumee.getName() + ". Weird.");
    }
  }

  /**
   * Listener-triggered from {@link PlayerActionsFragment}.
   */
  @Override
  public void save() {
    new SavePlayer().execute(mPlayer);
    mMapFragment.updateCommand(getString(R.string.command_save));
    mMapFragment.enterCurrentCommandWithResult("");
  }

  /*
   * AsyncTasks.
   */

  private class JoinGame extends AsyncTask<String, Void, Player> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      progressDialog.show();
    }

    @Override
    protected Player doInBackground(String... ids) {
      Player player = null;
      try {
        player = mService.players().joinGame(ids[0], ids[1]).execute();
      } catch (IOException e) {
        Log.d("GameActivity", "error: " + e.getMessage(), e);
        mException = true;
      }

      return player;
    }

    @Override
    protected void onPostExecute(Player player) {
      writeHandleToSharedPrefs(mHandle);
      progressDialog.dismiss();
      if (!mException) {  // no exception thrown
        if (player != null
            && !player.containsKey("error_message")) {  // endpoint return value was not null
          Log.i("GameActivity", "New player: " + player + " added to game " + player.getGameId());
          mPlayer = player;
          if (mPlayer.getPickups() == null) {
            mPlayer.setPickups(new ArrayList<Pickup>());
          }
          if (mPlayer.getBaseItems() == null) {
            mPlayer.setBaseItems(new ArrayList<Pickup>());
          }
          new NotifyJoin().execute(mGameId, mHandle);
        } else {
          Log.i("GameActivity", "The game either no longer exists or is already in progress.");
          OkDialogFragment dialog = new OkDialogFragment();
          dialog.setArguments(R.string.cannot_haz_dialog,
              "This game either no longer exists or has already started.");
          dialog.show(getFragmentManager(), "OkDialogListener");
        }
      } else {
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.cannot_haz_dialog, "Sorry, you are unable to join the game.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class NotifyJoin extends AsyncTask<String, Void, PlayerCollection> {
    private boolean mException = false;

    @Override
    protected PlayerCollection doInBackground(String... ids) {
      PlayerCollection players = null;
      try {
        players = mService.players().notifyJoin(ids[0], ids[1]).execute();
      } catch (IOException e) {
        Log.d("GameActivity", "NotifyJoin error: " + e.getMessage());
        mException = true;
      }
      return players;
    }

    @Override
    protected void onPostExecute(PlayerCollection players) {
      ArrayList<String> currentlyJoined = new ArrayList<String>();
      if (!mException  // no exception thrown
          && players != null
          && !players.containsKey("error_message")) {  // endpoint return value was not null
        if (players.containsKey("items")) {  // endpoint returned a non-empty list
          for (Player player : players.getItems()) {
            currentlyJoined.add(player.getHandle());
          }
        } else {
          Log.i("GameActivity", "No players have joined this game yet.");
        }
        mPreGameFragment.addCurrentPlayers(currentlyJoined);
        Log.i("GameActivity", "Players " + currentlyJoined + " have joined this game so far.");
      } else {
        Log.w("GameActivity", "Someting went wrong. Unable to find current players.");
      }
    }
  }

  private class GetPlayer extends AsyncTask<String, Void, Player> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      progressDialog.show();
    }

    @Override
    protected Player doInBackground(String... ids) {
      Player player = null;
      try {
        player = mService.players().get(ids[0]).execute();
      } catch (IOException e) {
        Log.d("GameActivity", "GetPlayer error: " + e.getMessage(), e);
        mException = true;
      }

      return player;
    }

    @Override
    protected void onPostExecute(Player player) {
      progressDialog.dismiss();
      if (!mException) {  // no exception thrown
        if (player != null
            && !player.containsKey("error_message")) {  // endpoint return value was not null
          Log.i("GameActivity", "Retrieved player: " + player);
          if (player.getPickups() == null) {
            player.setPickups(new ArrayList<Pickup>());
          }
          if (player.getBaseItems() == null) {
            player.setBaseItems(new ArrayList<Pickup>());
          }
          mPlayer = player;
          mGameId = player.getGameId();
          onGameResume();
        } else {
          Log.i("GameActivity", "No player found.");
          OkDialogFragment dialog = new OkDialogFragment();
          dialog.setArguments(R.string.cannot_haz_dialog, "This game no longer exists.");
          dialog.show(getFragmentManager(), "OkDialogListener");
        }
      } else {
        Log.i("GameActivity", "Something went wrong.");
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.setArguments(R.string.cannot_haz_dialog,
            "There was a problem retrieving your game. Try rejoining the game again.");
        dialog.show(getFragmentManager(), "OkDialogListener");
      }
    }
  }

  private class SavePlayer extends AsyncTask<Player, Void, Void> {
    private boolean mException = false;

    @Override
    protected void onPreExecute() {
      mActionsFragment.startGameSave();
    }

    @Override
    protected Void doInBackground(Player... players) {
      try {
        mService.players().update(players[0]).execute();
      } catch (IOException e) {
        Log.d("GameActivity", "SavePlayer error: " + e.getMessage(), e);
        mException = true;
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void player) {
      mActionsFragment.endGameSave();
      if (!mException) {
        Toast.makeText(GameActivity.this, R.string.toast_saved_game, Toast.LENGTH_SHORT).show();
        Log.i("GameActivity", "Saved player: " + player);
     } else {
        Toast.makeText(GameActivity.this, R.string.toast_not_saved_game, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private class StartGame extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... ids) {
      try {
        mService.games().start(ids[0], ids[1]).execute();
        Log.i("GameActivity", "Game " + ids[1] + " has started.");
      } catch (IOException e) {
        Log.d("GameActivity", "StartGame error: " + e.getMessage(), e);
      }
      return null;
    }
  }

  private class LeaveGame extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... ids) {
      try {
        mService.players().leaveGame(ids[0], ids[1]).execute();
        Log.i("GameActivity", "Player " + mHandle + " has left the game and self-destructed.");
      } catch (IOException e) {
        Log.d("GameActivity", "LeaveGame error: " + e.getMessage(), e);
      }
      return null;
    }
  }

  private class CancelGame extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... ids) {
      try {
        mService.games().cancel(mHandle, ids[0]).execute();
        Log.i("GameActivity", "Destroyed game: " + ids[0]);
      } catch (IOException e) {
        Log.d("GameActivity", "CancelGame error: " + e.getMessage(), e);
      }
      return null;
    }
  }

  private class EndGame extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... ids) {
      try {
        mService.games().end(ids[0], ids[1]).execute();
        Log.i("GameActivity", "Game " + ids[1] + " has ended.");
      } catch (IOException e) {
        Log.d("GameActivity", "EndGame error: " + e.getMessage(), e);
      }
      return null;
    }
  }

  private class SaveScoresAndSend extends AsyncTask<String, Void, PlayerCollection> {
    private boolean mException = false;

    @Override
    protected PlayerCollection doInBackground(String... scores) {
      PlayerCollection players = null;
      try {
        players = mService.players().saveAndSendScores(
            Long.parseLong(scores[4]), scores[1], Long.parseLong(scores[2]),
            scores[0], Long.parseLong(scores[3]))
            .execute();
      } catch (IOException e) {
        Log.d("GameActivity", "SaveScoresAndSend error: " + e.getMessage());
        mException = true;
      }
      return players;
    }

    @Override
    protected void onPostExecute(PlayerCollection result) {
      if (!mException  // no exception thrown
          && result != null
          && !result.containsKey("error_message")) {  // endpoint return value was not null
        if (result.containsKey("items")) {  // endpoint returned a non-empty list
          mScoresFragment.addPlayersScores(result.getItems());
        }
      } else {
        Log.w("GameActivity", "Someting went wrong. Unable to find current players.");
        mScoresFragment.reportRetrievalIssue();
        return;
      }
    }
  }
}
