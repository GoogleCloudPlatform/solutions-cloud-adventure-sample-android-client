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

package com.google.cloud.solutions.cloudadventure.world;

import com.google.api.services.cloudadventure.model.Player;
import com.google.api.services.cloudadventure.model.Tile;

import android.util.Log;

import java.util.List;

/**
 * Utility class to aid with map navigation in the game.
 */
public class MapUtils {
  /**
   * This class enumerates the types of cardinal directions available. Note for developers: be very
   * careful when changing the order of declaration in this enum, since values() is called elsewhere
   * in this application.
   * <p>
   * NOTE: Updates to this enum need to be reflected in the corresponding enum class in the backend
   */
  public enum Cardinal {
    NORTH, EAST, SOUTH, WEST
  }

  /**
   * This class maps the ever-changing relationships between the cardinal directions and the
   * relative directions of a {@link Player}. One of these mappers is instantiated for each
   * {@link Player} in the game, and is updated each time the player moves and changes direction.
   */
  public static class DirectionMapper {
    private Cardinal[] cardinalDirections = Cardinal.values();

    private Cardinal forward = Cardinal.NORTH;
    private Cardinal right = Cardinal.EAST;
    private Cardinal back = Cardinal.SOUTH;
    private Cardinal left = Cardinal.WEST;

    public DirectionMapper(final Cardinal initialFacing) {
      switch (initialFacing) {
        case NORTH:
          break;
        case EAST:
          turnRight();
          break;
        case SOUTH:
          turnAround();
          break;
        case WEST:
          turnLeft();
          break;
      }
    }

    public Cardinal getFrontCardinal() {
      return forward;
    }

    public Cardinal getRightCardinal() {
      return right;
    }

    public Cardinal getBehindCardinal() {
      return back;
    }

    public Cardinal getLeftCardinal() {
      return left;
    }

    public void turnRight() {
      forward = cardinalDirections[(forward.ordinal() + 1 + 4) % 4];
      right = cardinalDirections[(right.ordinal() + 1 + 4) % 4];
      back = cardinalDirections[(back.ordinal() + 1 + 4) % 4];
      left = cardinalDirections[(left.ordinal() + 1 + 4) % 4];
    }

    public void turnAround() {
      forward = cardinalDirections[(forward.ordinal() + 2 + 4) % 4];
      right = cardinalDirections[(right.ordinal() + 2 + 4) % 4];
      back = cardinalDirections[(back.ordinal() + 2 + 4) % 4];
      left = cardinalDirections[(left.ordinal() + 2 + 4) % 4];
    }

    public void turnLeft() {
      forward = cardinalDirections[(forward.ordinal() - 1 + 4) % 4];
      right = cardinalDirections[(right.ordinal() - 1 + 4) % 4];
      back = cardinalDirections[(back.ordinal() - 1 + 4) % 4];
      left = cardinalDirections[(left.ordinal() - 1 + 4) % 4];
    }
  }

  /**
   * Given a {@link Player} and cardinal direction, finds the next {@link Tile} in that direction.
   * 
   * @param player a {@link Player} with a current {@link Tile} location
   * @param direction the direction in which to find the next tile
   * @return {@link Tile}
   */
  public static Tile getNextTile(Player player, Cardinal direction) {
    Tile currentTile = player.getCurrentTile();
    List<List<Tile>> grid = player.getMaze().getGrid();
    try {
      switch (direction) {
        case NORTH:
          return grid.get(currentTile.getCoord().getX()).get(currentTile.getCoord().getY() + 1);
        case EAST:
          return grid.get(currentTile.getCoord().getX() + 1).get(currentTile.getCoord().getY());
        case SOUTH:
          return grid.get(currentTile.getCoord().getX()).get(currentTile.getCoord().getY() - 1);
        case WEST:
          return grid.get(currentTile.getCoord().getX() - 1).get(currentTile.getCoord().getY());
      }
    } catch (IndexOutOfBoundsException e) {
      Log.wtf("MapUtils", "Should not have been able to call this method with direction "
          + direction + ", because the UI should not have presented the player with the option.");
    }
    Log.wtf("MapUtils", "Why does an available direction exist such as " + direction + " here?");
    return currentTile;
  }
}
