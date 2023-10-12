package bots; // !! Do NOT change this package name !!

import com.tw.paintbots.GameManager;
import com.tw.paintbots.GameManager.SecretKey;
import com.tw.paintbots.AIPlayer;
import com.tw.paintbots.PlayerState;
import com.tw.paintbots.PlayerException;
import com.tw.paintbots.Board;
import com.tw.paintbots.Items.PowerUp;
import com.tw.paintbots.Items.PowerUpType;
import com.tw.paintbots.Items.PowerUp.Info;
import com.tw.paintbots.Items.ItemType;

import java.io.PrintWriter;

import java.util.Objects;
import java.util.List;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.concurrent.*;

import java.util.Random;
import com.badlogic.gdx.math.Vector2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

// =============================================================== //
class Chell extends AIPlayer {
  // --------------------------------------------------------------- //
  private Vector2 dir = new Vector2(-1.0f, 0.0f);

  // Game Info
  private Board gameBoard;
  private PlayerState me;
  private boolean admissionmode = false;
  private boolean initialization = true;
  private int player_idx = 0;

  // pre-calculated paths:
  private int amountOfGridPoints;
  private int distanceX = 0;
  private cell[][][] preCalcPaths;

  // Paintstation Locations
  private Vector<Vector2> paintstation = new Vector<Vector2>();
  private Vector<Vector2> colored_paintstation = new Vector<Vector2>();

  // AStar
  private HashMap<Vector2, Boolean> blockedList = new HashMap<Vector2, Boolean>();

  // Enemy Info
  private Boolean enemy2Close = false;
  private Vector2[] playerPositions = new Vector2[4];
  private float maxEnemyDist = 100;

  // TODO: Remove debugging variables for improved performance
  // Debugging
  private boolean debug_ = false;
  private int counter56 = 0;

  // precalc path
  private Stack<Vector2> preCalcCells = new Stack<Vector2>();
  private cell preCalcGoal = new cell();

  // A* pathfinding
  private Stack<Vector2> liveACells = new Stack<Vector2>();
  private cell gridPointGoal = new cell();
  private Vector2 subGoal = new Vector2(-1, -1);
  private int walkProgress = 0;
  private Vector2 goal = null;
  private boolean panic = false;
  private boolean validPath = false;
  private boolean atNextSubDestination = true;

  // WalkingSpeed for isSubGoal
  private boolean savelastPos = true;
  private Vector2 lastPos;
  private float walkingSpeed = 0.0f;
  private float rawWalkingSpeed = -1.0f;
  private boolean rawWalkingSpeedCheck = true;

  // roomba-Walking
  private Vector2 roombaPos;
  private boolean mode = true;
  private int subDir = 0;
  private int x = 50;
  private int paintradius = 40;

  // Stuck-Test
  private int stuckAlarm = 3;
  private int panicStuckAlarm = 10;
  private boolean panicMode = false;
  private Vector2 stuck;
  private Vector2 panicStuck;

  private boolean isStuck = false;

  // Recharging
  private boolean recharge = false;
  private boolean rechargeGoal = true;
  private boolean reachedGoalRecharge = false;
  private boolean dontRecharge = false;

  // PowerUps
  private boolean walkToPowerUp = false;
  private Vector2 powerUp = null;
  private PowerUpType curPowerUpType = null;
  private ArrayList<PowerUp> power_ups = new ArrayList<>();
  private ArrayList<Boolean> power_ups_active = new ArrayList<>();
  private ArrayList<Vector2> power_ups_pos = new ArrayList<>();
  private boolean firstPowerUpCheck = true;
  private boolean powerUpsValid = false;
  private double elapsed_time = 0.0;
  private int handledPowerUps = 0;

  // Random
  private Random rndom;
  private Random rndPU;

  // ======================= Player methods ===================== //
  /**
   * This method is required, so that the GameManager can set your initial
   * direction.
   *
   * @param dir - the direction in which the player should look
   * @param key - the SecretKey only available to the GameManager
   */
  @Override
  public void setInitialDirection(Vector2 dir, GameManager.SecretKey key) {
    Objects.requireNonNull(key);
    this.dir = dir;
  }

  // ======================= AIPlayer methods ====================== //
  //@formatter:off
  @Override public String  getBotName()   { return "Chell"; }
  @Override public String  getStudent()   { return "Tobias Fr."; }
  @Override public int     getMatrikel()  { return 013370; }
  @Override public Vector2 getDirection() { return dir; }
  //@formatter:on

  // ======================= TFTestBot2 methods ==================== //
  // !! Please provide this constructor but do NOT use it !! //
  // !! ....... Use the initBot() method instead ........ !! //
  public Chell() {
    /* !! leave this blank !! */ }

  // --------------------------------------------------------------- //
  // !! Please provide this constructor but do NOT use it !! /
  // !! ....... Use the initBot() method instead ........ !! /
  public Chell(String name) throws PlayerException {
    /* do not change this */
    super("AI-" + name);
  }

  // --------------------------------------------------------------- //
  /**
   * This method is called by the GameManager in each update loop. The
   * GameManager is the only class that can call this method.
   *
   * @param key - the SecretKey only available to the GameManager
   */
  //@formatter:off
  @Override
  public void update(GameManager.SecretKey secret) {
    Objects.requireNonNull(secret); // <= keep this line to avoid cheating
    myUpdate();                     // <= call your own method
    super.update(secret);           // <= keep this line for the animation
  }
  //@formatter:on

  // --------------------------------------------------------------- //
  /**
   * This method is called as soon as an instance of this bot for the actual
   * game is created. You can use this as a substitute for the constructor.
   */
  @Override
  public void initBot() {
    // initializing some stuff

    // Game Info
    gameBoard = GameManager.get().getBoard();
    admissionmode = GameManager.get().admissionMode();
    initialization = true;
    player_idx = getState().player_id;

    // pre-calculated paths:
    amountOfGridPoints = 50; // Increase for a lot and short astar calls; Decrease for less and long astar
                             // calls
    distanceX = 0;

    // Paintstation Locations
    paintstation = new Vector<Vector2>();
    colored_paintstation = new Vector<Vector2>();

    // AStar
    blockedList = new HashMap<Vector2, Boolean>();

    // Enemy Info
    enemy2Close = false;
    playerPositions = new Vector2[4];
    maxEnemyDist = 250;

    // TODO: Remove debugging variables for improved performance
    // Debugging
    debug_ = false;
    counter56 = 0;

    // precalc path
    preCalcCells = new Stack<Vector2>();
    preCalcGoal = new cell();

    // A* pathfinding
    liveACells = new Stack<Vector2>();
    gridPointGoal = new cell();
    subGoal = new Vector2(-1, -1);
    walkProgress = 0;
    goal = null;
    panic = false;
    validPath = false;
    atNextSubDestination = true;

    // WalkingSpeed for isSubGoal
    savelastPos = true;
    lastPos = null;
    walkingSpeed = 0.0f;
    rawWalkingSpeed = -1.0f;
    rawWalkingSpeedCheck = true;

    // roomba-Walking
    roombaPos = new Vector2(-100, -100);
    mode = false;
    subDir = 0;
    x = 50;
    paintradius = 40;

    // Stuck-Test
    stuckAlarm = 3;
    panicStuckAlarm = 10;
    stuck = null;
    panicStuck = null;
    isStuck = false;

    // Recharging
    recharge = false;
    rechargeGoal = true;
    reachedGoalRecharge = false;
    dontRecharge = false;

    // PowerUps
    walkToPowerUp = false;
    powerUp = null;
    curPowerUpType = null;
    power_ups = new ArrayList<>();
    power_ups_active = new ArrayList<>();
    power_ups_pos = new ArrayList<>();
    firstPowerUpCheck = true;
    powerUpsValid = false;
    elapsed_time = 0.0;
    handledPowerUps = 0;

    // Random
    rndom = new Random(3333);
    rndPU = new Random(GameManager.get().randomSeed());

    // --------------------------------------------
    generatePowerUps(14);
    // System.out.println("GENERATED POWERUPS");
    // for (int i = 0; i < power_ups.size(); i++) {
    // System.out.println(power_ups.get(i).getInfo().toString() + " POS: " +
    // power_ups_pos.get(i));
    // }

    // System.out.println(getState().paint_color);
    // System.out.println(getState().player_id);
    // ID0: GREEN
    // ID1: PURPLE
    // ID2: BLUE
    // ID3: ORANGE

    for (int i = 0; i < 1000; i++) {
      for (int j = 0; j < 1000; j++) {
        boolean passable = gameBoard.getType(i, j).isPassable(this);
        if (gameBoard.getType(i, j).getTypeID() == 10) { // general station
          paintstation.add(new Vector2(i, j));
        } else if (gameBoard.getType(i, j).getTypeID() == (11 +
            getState().player_id)) { // my station
          colored_paintstation.add(new Vector2(i, j));
        }
        if (passable == false) {
          blockedList.put(new Vector2(i, j), false);
        }
      }
    }
    // create GridPoints
    distanceX = gameBoard.getWidth() / amountOfGridPoints;
    // System.out.println("X-Abstand: " + distanceX);
    preCalcPaths = new cell[amountOfGridPoints][amountOfGridPoints][8];
    // long start = System.nanoTime();
    preCalcGridPaths(distanceX, false);
    // System.out.println("Time to precalc Paths: " + (System.nanoTime() - start));
    // System.out.println("-----");
    // System.out.println("DistanceX: " + distanceX);
    // System.out.println(preCalcPaths[0][0][4].toString());
    // System.out.println("-----");
    // System.out.println(getClosestGridPointVector2(new Vector2(-15, 1000)));
    // System.out.println(getClosestGridPointXY(new Vector2(0, 1000)));
    // System.out.println(isBlocked(316, 479));
    initialization = false;
    // System.out.println("---");
    // System.out.println(gameBoard.hashCode());
    preworkMap();
    // System.out.println("ID: " + player_idx);
    // System.out.println("---");

  }

  // TODO: more handselected positions
  public void preworkMap() {
    int hash = blockedList.hashCode();
    // System.out.println(getState().paint_color);
    // ID0: GREEN
    // ID1: PURPLE
    // ID2: BLUE
    // ID3: ORANGE
    // System.out.println(hash);
    switch (hash) {
      case 1447105388:
        // System.out.println("Level: Nothing Special");
        // System.out.println("Level 0");
        paintstation = new Vector<Vector2>();
        paintstation.add(new Vector2(559, 530));
        if (player_idx == 0) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(216, 206));
        }
        if (player_idx == 1) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(454, 205));
        }
        if (player_idx == 2) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(688, 205));
        }
        if (player_idx == 3) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(786, 205));
        }
        break;

      case -1859760512:
        // System.out.println("Level: Admission");
        // System.out.println("Level 1");
        paintstation = new Vector<Vector2>();
        paintstation.add(new Vector2(500, 470));
        break;

      // Blocked Corners
      case -2016231580:
      case 1406254948:
      case 1459945316:
      case -2069921948:
        // System.out.println("Level: Blocked Corners");
        // System.out.println("Level 2");
        if (player_idx != 0) {
          for (int i = 0; i < 386; i++) {
            for (int j = 0; j < 314; j++) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        if (player_idx != 1) {
          for (int i = 612; i < 1000; i++) {
            for (int j = 684; j < 1000; j++) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        if (player_idx != 2) {
          for (int i = 0; i < 387; i++) {
            for (int j = 1000; j > 684; j--) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        if (player_idx != 3) {
          for (int i = 612; i < 1000; i++) {
            for (int j = 314; j > -1; j--) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        if (player_idx == 0) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(182, 38));
        }
        if (player_idx == 1) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(49, 851));
        }
        if (player_idx == 2) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(808, 853));
        }
        if (player_idx == 3) {
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(805, 39));
        }
        break;

      case -1933956268:
        // System.out.println("Level: Happy Face");
        // System.out.println("Level 4");
        paintstation = new Vector<Vector2>();
        paintstation.add(new Vector2(522, 454));

        if (player_idx == 0) {// ID0: GREEN
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(151, 108));
        }
        if (player_idx == 1) {// ID1: PURPLE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(153, 837));
        }
        if (player_idx == 2) {// ID2: BLUE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(846, 110));
        }
        if (player_idx == 3) {// ID3: ORANGE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(849, 844));
        }
        break;
      case 2056709680:
        // System.out.println("Level: Symmetric Corners");
        // System.out.println("Level 5");
        paintstation = new Vector<Vector2>();
        paintstation.add(new Vector2(529, 434));

        if (player_idx == 0) {// ID0: GREEN
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(154, 107));
        }
        if (player_idx == 1) {// ID1: PURPLE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(840, 63));
        }
        if (player_idx == 2) {// ID2: BLUE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(158, 881));
        }
        if (player_idx == 3) {// ID3: ORANGE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(840, 890));
        }
        break;
      case -561291540:
        // System.out.println("Level: Tree Scale");
        // System.out.println("Level 6");
        if (player_idx == 0) {// ID0: GREEN
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(154, 107));
        }
        if (player_idx == 1) {// ID1: PURPLE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(156, 838));
        }
        if (player_idx == 2) {// ID2: BLUE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(844, 107));
        }
        if (player_idx == 3) {// ID3: ORANGE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(847, 845));
        }
        break;
      case 0:
        // System.out.println("Level: Contest 1,3 or 5");
        // System.out.println("Level 7 or 9 or 11");
        break;
      case 1152928960:
        // System.out.println("Level: Contest 2");
        // System.out.println("Level 8");
        // No need for optimizations, afaik
        break;

      // Contest 4 Level
      case -203684488:
      case 1297941880:
      case 817005944:
      case -1976334984:
        // System.out.println("Level: Contest 4");
        // System.out.println("Level 10");
        if (player_idx < 2) { // GREEN AND PURPLE
          if (player_idx == 0) {
            paintstation = new Vector<Vector2>();
            paintstation.add(new Vector2(100, 450));
          }

          if (player_idx == 1) {
            paintstation = new Vector<Vector2>();
            paintstation.add(new Vector2(100, 550));
          }

          for (int i = 675; i < 1000; i++) {
            for (int j = 450; j >= 372; j--) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        if (player_idx > 1) { // BLUE AND ORANGE
          if (player_idx == 2) {
            paintstation = new Vector<Vector2>();
            paintstation.add(new Vector2(925, 450));
          }

          if (player_idx == 3) {
            paintstation = new Vector<Vector2>();
            paintstation.add(new Vector2(925, 550));
          }

          for (int i = 0; i < 321; i++) {
            for (int j = 638; j >= 367; j--) {
              blockedList.put(new Vector2(i, j), false);
            }
          }
        }
        break;
      case -2021196176:
        // System.out.println("Level: aud");
        // System.out.println("Level 12");
        if (player_idx == 0) {// ID0: GREEN
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(151, 107));
        }
        if (player_idx == 1) {// ID1: PURPLE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(154, 842));
        }
        if (player_idx == 2) {// ID2: BLUE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(847, 107));
        }
        if (player_idx == 3) {// ID3: ORANGE
          colored_paintstation = new Vector<Vector2>();
          colored_paintstation.add(new Vector2(849, 845));
        }
        break;
      default:
        // System.out.println("Unknown Level");
        // System.out.println("Reason: Level is changed/new and could not be identified
        // and optimized.");
        break;
    }
  }

  // --------------------------------------------------------------- //
  /** This is a helper method called in the update method. */
  public void myUpdate() {
    me = getState();
    elapsed_time = GameManager.get().getElapsedTime();
    paintradius = me.paint_radius;
    // int save = (int)System.nanoTime();
    // System.out.println(player_idx+": " + dir);
    stuckTest();

    if (savelastPos) {
      savelastPos = false;
      lastPos = me.pos;
    } else {
      rawWalkingSpeed = lastPos.dst2(me.pos);
      if (rawWalkingSpeed > 5.0f) {
        walkingSpeed = rawWalkingSpeed;
        rawWalkingSpeedCheck = true;
      }
      savelastPos = true;
    }
    checkPowerUpDeath();
    checkPowerUp();
    if (powerUp == null) {
      if (powerUpsValid == false) {
        powerUpSpawned();
      } else {
        determinePowerUp();
      }
    } else {
      // System.out.println("Active: " + isActivePowerUp(powerUp));
      if (powerUpsValid == false || firstPowerUpCheck) {
        if (goal != null && goal.dst2(powerUp) < 2.0f && !isActivePowerUp(powerUp)) {
          // System.out.println("Goal is equal to PowerUP");
          // System.out.println("Power up delete");
          powerUp = null;
          walkToPowerUp = false;
          goal = null;
          mode = false;
          subDir = 0;
          validPath = false;
          firstPowerUpCheck = false;
          power_ups_active.set(handledPowerUps, false);
        }
      } else {
        if (powerUpsValid == true && goal != null) {
          for (int i = 0; i < power_ups.size(); i++) {
            // System.out.println("Dst to PowerUP " + i +" is dead: " +
            // power_ups_active.get(i)+ ": " + (power_ups_pos.get(i).dst2(powerUp)));
            // System.out.println("PowerUP: " + handledPowerUps + " Pos: " + powerUp);
            if (power_ups_active.get(i) == false && power_ups_pos.get(i).dst2(powerUp) < 2.0f) {
              powerUp = null;
              walkToPowerUp = false;
              goal = null;
              validPath = false;
              break;
            }
          }
        }
      }
    }

    determineGoal();

    // goal = new Vector2(503,409);
    if (goal == null) { // Walking without A*
      // System.out.println("Walking in Roomba Mode.");
      subGoal = new Vector2(-1, -1);
      roomba();
    } else { // Walking with A*
      // System.out.println("Determined Goal:" + goal);
      // if the path is NOT valid
      if (!validPath) {
        if (walkProgress == 0) { // if we are at the first stage of A*-Walking
          try {
            getPath(me.pos, goal);
            // System.out.println("Path is generated in Stage One.");
            validPath = true;
            atNextSubDestination = true;
            return;
          } catch (Exception e) {

            panic = true;
            validPath = false;
            goal = null;
            // System.out.println(e.toString());
            e.printStackTrace();
            // System.out.println("Stage One failed.");
            return;
          }
        } else if (walkProgress == 1) { // if we are at the second stage of A*-Walking
          try {
            getPath(me.pos, goal);
            validPath = true;
            atNextSubDestination = true;
            // System.out.println("Path is generated in Stage Two and walked in Stage
            // One.");
            // System.out.println("Grid-Waypoints: " + preCalcCells.size());
            return;
          } catch (Exception e) { // if A* takes too long

            // System.out.println(e.toString());
            e.printStackTrace();
            // System.out.println("Stage Two failed.");
            panic = true;
            validPath = false;
            goal = null;
            return;
          }
        } else if (walkProgress == 2) {
          try {
            getPath(me.pos, goal);
            validPath = true;
            atNextSubDestination = true;
            // System.out.println("Path is generated in Stage Three and walked in Stage One
            // and Two.");
            return;
          } catch (Exception e) { // if A* takes too long

            // System.out.println(e.toString());
            e.printStackTrace();
            // System.out.println("Stage Three failed.");
            panic = true;
            validPath = false;
            goal = null;
            return;
          }
        }
      }

      if (walkProgress == 0 && liveACells.empty() && validPath) {
        // System.out.println("Switchting to Stage Two in A*.");
        validPath = false;
        walkProgress = 1;
      }

      // if in stage two of A*-Walking get next batch of paths
      // TODO: create just one big path instead of multiple small ones
      // big path could be reduced like every small path
      if (walkProgress == 1 && liveACells.empty() && !preCalcCells.empty()) {
        // System.out.println("Generating Sub-Path");
        Vector2 clue = preCalcCells.pop(); // Get Point A
        Vector2 id = getClosestGridPointXY(clue); // Get the closest GridPointID to Point A
        if (!preCalcCells.empty()) { // If this is not the last Point
          Vector2 GridPointB = preCalcCells.peek(); // Peek at Point B
          Vector2 id2 = getClosestGridPointXY(GridPointB); // Get closest GridPointID to Point B

          cell output = null; // Create new cell
          for (int i = 0; i < 8; i++) { // Go through all Paths at GridPoint A

            if (preCalcPaths[(int) id.x][(int) id.y][i] != null) { // If the path exists
              // Get Vector of cell that is at the end of a Path at GridPoint A
              Vector2 CellPointA = new Vector2(preCalcPaths[(int) id.x][(int) id.y][i].x_,
                  preCalcPaths[(int) id.x][(int) id.y][i].y_);
              // If the distance from the CellPointA to the actual goal GridPointB small
              // enough is
              if (CellPointA.dst(GridPointB) < 1.0) {
                output = preCalcPaths[(int) id.x][(int) id.y][i];
                break;
              }
            }
          }
          if (output != null) {
            liveACells = createPath(output, false);
          } else {
            // System.out.println("Skipped: " + preCalcCells.size());
          }
          // System.out.println("Stage 2 cells: " + liveACells.size());
        }
        // System.out.println("After generating Sub-Path: " + liveACells.size() + "
        // Waypoints.");
      }

      if (walkProgress == 1 && preCalcCells.empty() && liveACells.empty() && validPath) {
        // System.out.println("Switchting to Stage Three in A*.");
        validPath = false;
        walkProgress = 2;
      }

      if (walkProgress == 2 && isSubGoal(me.pos, goal, walkingSpeed)) {

        // System.out.println("Reached Goal in A*.");
        // System.out.println(me.pos);
        if (powerUpsValid && !firstPowerUpCheck) {
          validPath = false;
          walkProgress = 0;
          if (recharge) {
            reachedGoalRecharge = true;
          }
          if (powerUp != null && goal.dst2(powerUp) < 100.0f && isSpawned()) {
            powerUp = null;
            power_ups_active.set(handledPowerUps, false);
            // System.out.println("Setting " + handledPowerUps + " to false: " +
            // power_ups_active.get(handledPowerUps));
          }
          subDir = 0;
          mode = false;
          goal = null;
          // if(!recharge && walkToPowerUp && !isSpawned()) {
          // //System.out.println("Picked Up Power UP; moving somewhere else");
          // findRandomGoal();
          // }
          walkToPowerUp = false;
        } else {
          validPath = false;
          walkProgress = 0;
          if (recharge) {
            reachedGoalRecharge = true;
          }
          goal = null;
          walkToPowerUp = false;
        }
      }

      // if there are more subGoals and we are at the previous subGoal: set new
      // subGoal
      if (!liveACells.empty() && atNextSubDestination) {
        subGoal = liveACells.pop();
      }

      if (subGoal.x != -1 && subGoal.y != -1) { // Set direction towards subGoal and check if at subGoal
        // Update Direction only if we are correcting the current Direction or if we are
        // the subgoal
        Vector2 subGoalDir = new Vector2(subGoal);
        subGoalDir.sub(me.pos);
        // if((int)dir.angleDeg()!=(int)subGoalDir.angleDeg()) {
        dir.setAngleDeg(subGoalDir.angleDeg());
        // }

        // System.out.println("---");
        // System.out.println("Walking Speed: " + walkingSpeed);
        // System.out.println("Distance to SubGoal: " + me.pos.dst2(subGoal));
        // System.out.println("Walking from: " + me.pos +" to: " + subGoal);
        // System.out.println("Current Dir: " + dir.angleDeg());
        // System.out.println("At SubGoal: " + atNextSubDestination);
      }
    }
    atNextSubDestination = isSubGoal(me.pos, subGoal, walkingSpeed);

    // System.out.println("Time Bot-Update: " + ((int)System.nanoTime()-save));
  }

  public boolean isSpawned() {
    for (int i = 0; i < power_ups.size(); i++) {
      Vector2 buff_pos = new Vector2(power_ups_pos.get(i));
      if (powerUp != null && buff_pos.dst2(powerUp) < 100f && power_ups_active.get(i) == true
          && power_ups.get(i).getInfo().spawn_time < elapsed_time) {
        return true;
      }
    }
    return false;
  }

  // This is from the source code of the game and is slightly altered
  private void generatePowerUps(int count) {
    int game_time = 120;
    int spawn_delta = game_time / (count + 2); // time between two spawns
    // ---
    int spawn_time = 0;
    for (int i = 0; i < count; ++i) {
      spawn_time += spawn_delta;
      // --- random time
      int time_off = (int) (((rndPU.nextDouble() * spawn_delta) - spawn_delta) / 2);
      int life_time = (int) (((rndPU.nextDouble() * spawn_delta) + spawn_delta));
      life_time = Math.max(life_time, 15);
      // --- random type
      int rnd_idx = (int) (rndPU.nextDouble() * (PowerUpType.getTypeCount() + 1));
      PowerUpType type = PowerUpType.idxToType(rnd_idx);
      // --- create the power up
      PowerUp power_up = new PowerUp(type, spawn_time + time_off, life_time);
      // --- random position
      int[] pos = generatePowerUpPosition();
      // power_up.setPosition(new Vector2(pos[0], pos[1]), secret_lock);
      power_ups_pos.add(new Vector2(pos[0], pos[1]));
      power_ups_active.add(true);
      power_ups.add(power_up);
    }
  }

  private int[] generatePowerUpPosition() {
    int brd_width = gameBoard.getWidth();
    int brd_height = gameBoard.getHeight();
    int offset = 100;
    int rnd_x = 0;
    int rnd_y = 0;
    do {
      rnd_x = (int) (rndPU.nextDouble() * (brd_width - offset) + offset / 2);
      rnd_y = (int) (rndPU.nextDouble() * (brd_height - offset) + offset / 2);
    } while (gameBoard.getType(rnd_x, rnd_y) != ItemType.NONE);
    int[] output = { rnd_x, rnd_y };
    return output;
  }

  public Stack<Vector2> createPath(cell goalCell, boolean debug) {
    Stack<Vector2> pathPoints = new Stack<Vector2>();
    // long start = System.nanoTime();
    if (goalCell != null) { // goalCell is not null
      pathPoints = new Stack<Vector2>();
      cell curCell = goalCell;
      cell curCellParent = curCell.parent_;
      int dirX = clamp(curCell.x_ - curCellParent.x_, -1, 1);
      int dirY = clamp(curCell.y_ - curCellParent.y_, -1, 1);
      Boolean first = true;
      int x = 0;
      while (curCell != null) {
        curCellParent = curCell.parent_;
        if (curCellParent != null) {
          if (x == 4 || first || (dirX != clamp(curCell.x_ - curCellParent.x_, -1, 1)
              && dirY != clamp(curCell.y_ - curCellParent.y_, -1, 1))) {
            first = false;
            pathPoints.push(new Vector2(curCell.x_, curCell.y_));
            dirX = clamp(curCell.x_ - curCellParent.x_, -1, 1);
            dirY = clamp(curCell.y_ - curCellParent.y_, -1, 1);
            x = 0;
          } else {
            x++;
          }
        } else {
          pathPoints.push(new Vector2(curCell.x_, curCell.y_));
        }
        curCell = curCellParent;
      }
      if (debug) {
        // System.out.println(pathPoints.size() + " waypoints.");
      }
    } else {
      // System.out.println("Tried creating a Path with a null cell.");
      // System.out.println("Goal was: " + goal);
    }

    if (debug) {
      // System.out.println("Creating Path took: " + (System.nanoTime() - start));
    }
    return pathPoints;
  }

  // Status: unknown, may work, my need some work
  // Needed for creation of big path
  public Stack<Vector2> createWholeCalcPath(cell preCalcGoal, boolean debug) {
    Stack<Vector2> pathPoints = new Stack<Vector2>();
    // along start = System.nanoTime();
    if (preCalcGoal != null) { // preCalcGoal is not null
      cell curCell = preCalcGoal;
      while (curCell != null) {
        Vector2 clue = new Vector2(curCell.x_, curCell.y_); // Get Point A
        Vector2 id = getClosestGridPointXY(clue); // Get the closest GridPointID to Point A
        if (curCell.parent_ != null) {
          Vector2 GridPointB = new Vector2(curCell.parent_.x_, curCell.parent_.y_); // Peek at Point B
          Vector2 id2 = getClosestGridPointXY(GridPointB); // Get closest GridPointID to Point B
          cell output = new cell(); // Create new cell
          for (int i = 0; i < 8; i++) { // Go through all Paths at GridPoint A
            if (preCalcPaths[(int) id.x][(int) id.y][i] != null) { // If the path exists
              // Get Vector of cell that is at the end of a Path at GridPoint A
              Vector2 CellPointA = new Vector2(preCalcPaths[(int) id.x][(int) id.y][i].x_,
                  preCalcPaths[(int) id.x][(int) id.y][i].y_);
              // If the distance from the CellPointA to the actual goal GridPointB small
              // enough is
              if (CellPointA.dst2(GridPointB) < 1.0) {
                output = preCalcPaths[(int) id.x][(int) id.y][i];
                break;
              }
            }
          }
          pathPoints.addAll(createPath(output, false));
        }
        curCell = curCell.parent_;
      }
      if (debug) {
        // System.out.println(pathPoints.size() + " waypoints.");
      }
    }
    // aSystem.out.println("Creating whole precalc-SubPath took: " +
    // (System.nanoTime() - start));
    return pathPoints;
  }

  public Stack<Vector2> createCalcPath(cell preCalcGoal, boolean debug) {
    Stack<Vector2> pathPoints = new Stack<Vector2>();
    // along start = System.nanoTime();
    if (preCalcGoal != null) { // preCalcGoal is not null
      cell curCell = preCalcGoal;
      while (curCell != null) {
        pathPoints.push(new Vector2(curCell.x_, curCell.y_));
        curCell = curCell.parent_;
      }
      if (debug) {
        // System.out.println(pathPoints.size() + " waypoints.");
      }
    }
    // aSystem.out.println("Creating precalc-SubPath took: " + (System.nanoTime() -
    // start));
    return pathPoints;
  }

  public boolean isValid(int x, int y) {
    int width = gameBoard.getWidth();
    int height = gameBoard.getHeight();
    if ((x >= 0 && x <= width) && (y >= 0 && y < height)) {
      return true;
    }
    return false;
  }

  public boolean isBlocked(int x, int y) {
    if (blockedList.containsKey(new Vector2(x, y)) == true) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isDestination(int x, int y, Vector2 dest, int delta) {
    // This needs to be delta>=res otherwise it wont find the destination
    // int delta = 15; <- size of circle around destination //standard: 5
    Vector2 source = new Vector2(x, y);
    if (source.dst2(dest) < delta * delta) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isSubGoal(Vector2 source, Vector2 dest, float delta) {
    // This needs to be delta>=res otherwise it wont find the destination
    // float delta = 6; // <- size of circle around destination //standard: 5
    if (delta == 0) {
      delta = 6.0f;
    } else {
      // delta+=0.1f;
    }
    if (source.dst2(dest) <= delta) {
      return true;
    } else {
      return false;
    }
  }

  public double calculateHeuristic(int x, int y, Vector2 dest) {
    Vector2 pos = new Vector2(x, y);
    return pos.dst(dest);
  }

  public boolean findNewDestination(Vector2 dest) {
    // System.out.println("A new destination needs to be found.");
    Vector2 save = new Vector2(dest.x, dest.y);

    Vector2 old_pos = getState().pos;
    int maxtries = 5; // maximum amount of tries to find new destination in 8 directions
    int tries = 0; // current tries
    int steps = 5; // test tries
    boolean xmode = true;
    boolean ymode = false;

    while (tries <= maxtries && isBlocked((int) dest.x, (int) dest.y) && (ymode || xmode)) {
      if (tries == maxtries) {
        if (xmode && !ymode) { // Step one: Try on the x-Axis
          tries = 0;
          dest = new Vector2(save.x, save.y);
          xmode = false;
          ymode = true;
        } else if (ymode && !xmode) { // Step two: Try on the y-Axis
          tries = 0;
          dest = new Vector2(save.x, save.y);
          xmode = true;
          ymode = true;
        } else if (ymode && xmode) { // Step three: Try on the diagonals
          tries = 0;
          dest = new Vector2(save.x, save.y);
          xmode = false;
          ymode = false;
        }
      }

      if (xmode) {
        if (old_pos.x < (int) dest.x) {
          dest.x -= steps;
        }
        if (old_pos.x > (int) dest.x) {
          dest.x += steps;
        }
      }
      if (ymode) {
        if (old_pos.y < (int) dest.y) {
          dest.y += steps;
        }
        if (old_pos.y > (int) dest.y) {
          dest.y -= steps;
        }
      }
    }
    if (isBlocked((int) dest.x, (int) dest.y)) {
      System.err.println("No alternative destination has been found.");
      return false;
    }

    return true;
  }

  private class cell implements Comparable<cell> {

    cell parent_;
    int x_;
    int y_;
    double f_;
    double g_;
    double h_;

    cell() {
      parent_ = null;
      x_ = -1;
      y_ = -1;
      f_ = Double.MAX_VALUE;
      g_ = 0;
      h_ = Double.MAX_VALUE;
    }

    cell(cell parent, int x, int y, double f, double g, double h) {
      parent_ = parent;
      x_ = x;
      y_ = y;
      f_ = f;
      g_ = g;
      h_ = h;
    }

    @Override
    public String toString() {
      if (parent_ != null) {
        return "X: " + x_ + ", Y: " + y_ + " Parent: [" + parent_.toString() + "]";
      } else {
        return "X: " + x_ + ", Y: " + y_ + " Parent: [NONE]";
      }
    }

    @Override
    public int compareTo(cell n) {
      return Double.valueOf(f_).compareTo(Double.valueOf(n.f_));
    }

    @Override
    public int hashCode() {
      // Implemented to get rid of warning and to have no effect on anything.
      return Objects.hash(x_, y_); // This is used in the PriorityQueue.
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return false;
      }
      if (other == this) {
        return true;
      }
      if (!(other instanceof cell)) {
        return false;
      }
      cell otherCell = (cell) other;

      if (this.x_ == otherCell.x_ && this.y_ == otherCell.y_) {
        return true;
      } else {
        return false;
      }
    }
  }

  //@formatter:off
  public cell aStarSearch(Vector2 source, Vector2 dest, int res,double hMax,boolean findNewDest, boolean debug) throws TimeoutException {
    long start = System.nanoTime();
    cell output = new cell();
    HashMap<Vector2,cell> closedList = new HashMap<Vector2,cell>();
    HashMap<Vector2,cell> openListCheck = new HashMap<Vector2,cell>();
    PriorityQueue<cell> openList = new PriorityQueue<cell>();
    int sourcex = (int)source.x;
    int sourcey = (int)source.y;

    int[][] neighbors = {{res,0},{0,res},{res,res},{-1*res,0},{0,-1*res},{-1*res,-1*res},{-1*res,1*res},{1*res,-1*res}};

    if(isValid(sourcex, sourcey) == false) {
      if(debug){
        System.err.println("The source is not valid.");
      }
      return null;
    }
    if(isValid((int)dest.x, (int)dest.y) == false) {
      if (debug) {
        System.err.println("The destination is not valid.");
      }
      return null;
    }

    if(isBlocked(sourcex,sourcey)) {
      if (debug) {
        System.err.println("The source is blocked.");
      }
      return null;
    }
                                              //if findNewDest = true find new dest or return true
    if(isBlocked((int)dest.x,(int)dest.y) && (findNewDest?findNewDestination(dest)==false:true)) {
       if (debug) {
        System.err.println("The destination is blocked and no new destination has been found.");
       }
      return null;
    }

    if(isDestination(sourcex,sourcey,dest,res)) {
      if (debug) {
      //System.out.println("The Bot is at the destination.");
      }
      return null;
    }

    cell startingCell = new cell(null, sourcex, sourcey, 0,0,0);
    closedList.put(new Vector2(sourcex,sourcey),startingCell);
    openList.add(startingCell); // Start-Zelle in die openList

    while(!openList.isEmpty()) {
      cell curCell = openList.remove();
      cell successor;

      if (hMax == 0 || curCell.h_<=hMax) {
        //Stack<Vector2> successorStack = findSuccessor(currCell, source, dest);
        for(int n[] : neighbors) { //look at the surrounding cells
          if(n==null) {
            break;
          }
          if(!initialization&&System.nanoTime()-start>4000000) {
            throw new TimeoutException("Astar took too long");
          }
          counter56++; //only for debugging to check how many cells have been looked at
          successor = new cell(curCell, curCell.x_+n[0],curCell.y_+n[1],0,0,0); // neighbor cell is initialized
          successor.g_ = curCell.g_ + 1.0 /*+ calculateGValue(successor.x_, successor.y_)*/; // new g value for the neigbor cell
          successor.h_ = calculateHeuristic(successor.x_,successor.y_,dest); // new h value for the neighbor cell
          successor.f_ = successor.g_ + successor.h_; //new f value
          //successor = new cell(curCell, curCell.x_+n[0],curCell.y_+n[1],fNew,gNew,hNew);
          if(isValid(successor.x_, successor.y_)) { //if the surrounding cell is valid -> in the map
            //System.out.println(closedList.get(new Vector2(successor.x_,successor.y_)));
            if(isDestination(successor.x_,successor.y_,dest,res)) { //if the neighbor cell is the destination
              output = successor;//new cell(curCell,curCell.x_+n[0],curCell.y_+n[1],0,0,0); //set the goalCell to the neighbor
              if (debug) {
                //aSystem.out.println("Finding Path took: " + (System.nanoTime() -start));
              }
              return output; //end the search
            } else if(!closedList.containsKey(new Vector2(successor.x_,successor.y_)) && !isBlocked(successor.x_,successor.y_)) { //if the neighbor cell is NOT on the CLOSED list AND the neighbor cell is NOT blocked.
              //!closedList.contains(new Vector2(successor.x_,successor.y_))==null &&
              if(openList.contains(successor)==false) {
                openList.add(successor);
                openListCheck.put(new Vector2(successor.x_,successor.y_),successor);
              } else {
                //while testcell!=null openListcheck.get
                cell testCell = openListCheck.get(new Vector2(successor.x_,successor.y_));
                if(testCell!=null && testCell.f_>successor.f_) {
                  openList.remove(testCell);
                  openListCheck.remove(testCell);
                  openList.add(successor);
                  openListCheck.put(new Vector2(successor.x_,successor.y_),successor);
                }
              }
            }
          }
        }
      }
      closedList.put(new Vector2(curCell.x_,curCell.y_),curCell);
    }

    if(debug) {
    //System.out.println("Could not find path from:" + source + " to " + dest);
    }
    return null;
  }
  //@formatter:on

  // Math.max and Math.min have been replaced with my own implementation
  public int clamp(int val, int min, int max) {
    return max(min, min(max, val));
  }

  // Simple get min method
  public int min(int a, int b) {
    if (a <= b) {
      return a;
    } else {
      return b;
    }
  }

  // Simple get max method
  public int max(int a, int b) {
    if (a >= b) {
      return a;
    } else {
      return b;
    }
  }

  // Calculate paths between GridPoints
  public void preCalcGridPaths(int distanceX, boolean debug) {
    int[][] neighbors = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 }, { 1, -1 } };
    for (int i = 0; i < preCalcPaths.length; i++) {
      for (int j = 0; j < preCalcPaths[i].length; j++) {
        for (int k = 0; k < neighbors.length; k++) {
          if (neighbors[k] == null) {
            break;
          }
          // System.out.println("i: "+i + "j: "+j + "k: " +k);
          if ((i + neighbors[k][0]) >= 0 && (i + neighbors[k][0]) < preCalcPaths[i].length && (j + neighbors[k][1]) >= 0
              && (j + neighbors[k][1]) < preCalcPaths[i].length) {
            if (((j + neighbors[k][1] < preCalcPaths.length && j + neighbors[k][1] >= 0))
                && (i + neighbors[k][0] < preCalcPaths.length && i + neighbors[k][0] >= 0)) {
              // calculate Path cell from current GridPoint to neighbor Gridpoint in all
              // directions

              // double maxH = calculateHeuristic(i*distanceX, j*distanceX, new Vector2((i +
              // neighbors[k][0])*distanceX,(j + neighbors[k][1])*distanceX));
              try {
                preCalcPaths[i][j][k] = aStarSearch(
                    new Vector2((i * distanceX) + (distanceX / 2), (j * distanceX) + (distanceX / 2)),
                    new Vector2(((i + neighbors[k][0]) * distanceX) + (distanceX / 2),
                        ((j + neighbors[k][1]) * distanceX) + (distanceX / 2)),
                    1, 0, false, debug);
              } catch (Exception e) {
                preCalcPaths[i][j][k] = null;
                // System.out.println("Precalculating Path failed for this point.");
              }
              // if(preCalcPaths[i][j][k]==null) {
              // System.out.println(i+"|"+j+"|"+k+"<- has no path");
              // }
              // if (k == 7 && i == amountOfGridPoints - 1) {
              // System.out.println(preCalcPaths[i][j][k].toString());
              // }
            }
          }
        }
      }
    }
  }

  public Vector2 getGridPointVector(int i, int j) {
    if (i >= amountOfGridPoints) {
      i = amountOfGridPoints - 1;
    } else if (i < 0) {
      i = 0;
    }
    if (j >= amountOfGridPoints) {
      j = amountOfGridPoints - 1;
    } else if (j < 0) {
      j = 0;
    }

    i = (distanceX * i) + (distanceX / 2);
    j = (distanceX * j) + (distanceX / 2);
    return new Vector2(i, j);
  }

  public Vector2 getClosestGridPointVector2(Vector2 source) {
    int x_ = (int) ((source.x - (distanceX / 2)) / distanceX);
    if ((source.x - (distanceX / 2)) % distanceX >= (distanceX / 2)) {
      x_++;
    }
    int y_ = (int) ((source.y - (distanceX / 2)) / distanceX);
    if ((source.y - (distanceX / 2)) % distanceX >= (distanceX / 2)) {
      y_++;
    }
    Vector2 output = getGridPointVector(x_, y_);
    int counter = 0;
    int[][] neighbors = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 }, { 1, -1 } };
    while (isBlocked((int) output.x, (int) output.y) && counter < 8) {
      output = getGridPointVector(x_ + neighbors[counter][0], y_ + neighbors[counter][1]);
      counter++;
    }
    return output;
  }

  public Vector2 getClosestGridPointXY(Vector2 source) {
    int x_ = (int) ((source.x - (distanceX / 2)) / distanceX);
    if ((source.x - (distanceX / 2)) % distanceX >= (distanceX / 2)) {
      x_++;
    }
    int y_ = (int) ((source.y - (distanceX / 2)) / distanceX);
    if ((source.y - (distanceX / 2)) % distanceX >= (distanceX / 2)) {
      y_++;
    }
    if (x_ >= amountOfGridPoints) {
      x_ = amountOfGridPoints - 1;
    } else if (x_ < 0) {
      x_ = 0;
    }
    if (y_ >= amountOfGridPoints) {
      y_ = amountOfGridPoints - 1;
    } else if (y_ < 0) {
      y_ = 0;
    }
    return new Vector2(x_, y_);
  }

  // Determines where to go to with A*
  public void determineGoal() {
    resetRecharge();
    if ((recharge || needsRecharge()) && !walkToPowerUp && !dontRecharge/* && !isStuck */) {
      if (rechargeGoal) {
        // System.out.println("Recharging");
        validPath = false;
      }
      goal = findClosePaintStation();
      rechargeGoal = false;
      // System.out.println(goal);
    } else if (powerUp != null && !recharge) {
      if (!walkToPowerUp) {
        // System.out.println("Found PowerUp -> Going to PowerUp: " + powerUp);
        walkToPowerUp = true;
        validPath = false;
      }
      goal = powerUp;
    } else if (goal == null) {
      isStuck = false;
      // System.out.println("Changing to Roomba-Mode");
    } else {
      // System.out.println("Goal: " + goal);
      // System.out.println("Walking with A*");
    }
    if (!reachedGoalRecharge /* || (!rechargeGoal && me.pos.dst2(goal)>2500) */) {
      stuckTest2();
    }
  }

  // TODO: find clever method to find free spaces instead of this method
  public void findRandomGoal() {
    int xLimit = 0;
    int yLimit = 0;

    goal = new Vector2(rndom.nextFloat() * (1000 - xLimit), rndom.nextFloat() * (1000 - yLimit));
    while (!isValid((int) goal.x, (int) goal.y) || isBlocked((int) goal.x, (int) goal.y)
        || GameManager.get().getCanvas().getColorUpturn((int) goal.x, (int) goal.y).getColorID() == me.paint_color
            .getColorID()
        || goal.dst2(me.pos) < 250 /* || !checkEnemies(goal) */) {
      goal = new Vector2(rndom.nextFloat() * 1000, rndom.nextFloat() * 1000);
    }
  }

  public void getPath(Vector2 source, Vector2 goal) {
    Vector2 startGridPoint = getClosestGridPointVector2(source);
    Vector2 endGridPoint = getClosestGridPointVector2(goal);
    if (walkProgress == 0) { // walk to GridPoint
      try {
        gridPointGoal = aStarSearch(me.pos, startGridPoint, 1, 0, false, false);
      } catch (Exception e) {
        // System.out.println(e.getMessage());

      }
      liveACells = createPath(gridPointGoal, false);
      // System.out.println(gridPointGoal.toString());
    } else if (walkProgress == 1) { // walk along the GridPoints
      // System.out.println("End Grid Point: " + endGridPoint);
      try {
        preCalcGoal = aStarSearch(startGridPoint, endGridPoint, distanceX, 0, false, false);
      } catch (Exception e) {
        // System.out.println(e.getMessage());

      }
      // For multiple SubPath
      preCalcCells = createCalcPath(preCalcGoal, false);

      // For one big Subpath
      // liveACells=createWholeCalcPath(preCalcGoal, true);
      // System.out.println("After creating full path: " + liveACells.size());
    } else if (walkProgress == 2) { // walk from GridPoint to Goal
      try {
        gridPointGoal = aStarSearch(endGridPoint, goal, 1, 0, false, false);
      } catch (Exception e) {

        // System.out.println(e.getMessage());
      }
      liveACells = createPath(gridPointGoal, false);
    }
  }

  public boolean isActivePowerUp(Vector2 source) {
    ArrayList<PowerUp.Info> list = GameManager.get().getActivePowerUps();
    if (isBlocked((int) source.x, (int) source.y)) {
      return false;
    }
    for (Info i : list) {
      if (i != null && source != null && i.position[0] == (int) source.x && i.position[1] == (int) source.y) {
        return true;
      }
    }
    return false;
  }

  // Get first spawned powerUp
  public void powerUpSpawned() {
    // System.out.println("PowerUps: " + getPowerUpCount());
    ArrayList<PowerUpType> playerList = getPowerUps();
    ArrayList<PowerUp.Info> list = GameManager.get().getActivePowerUps();
    for (Info info : list) {
      Vector2 pUPos = new Vector2(info.position[0], info.position[1]);
      if (pUPos.dst2(me.pos) < 750 * 750) {
        boolean pickup = false;
        // for (PowerUpType powerUpType : playerList) {
        // if I already have it or its instant or i have space
        if (/* info.type.getTypeID() == powerUpType.getTypeID() || */ info.type.getTypeID() > 3
            || getPowerUpCount() < 2) {
          pickup = true;
          if (true && firstPowerUpCheck) {
            if (comparePowerUpInfo(0, info)) {
              System.out.println("---PowerUps are valid!----");
              handledPowerUps++;
              powerUpsValid = true;
            } else {
              System.out.println("---PowerUps are NOT valid!----");
              powerUpsValid = false;
            }
          }
        }
        // }

        if (pickup/* || getPowerUpCount() != 2 */) {
          powerUp = pUPos;
          return;
        }
      }
    }
    powerUp = null;
  }

  // Checks if precalculated PowerUps are correct
  public boolean comparePowerUpInfo(int x, PowerUp.Info info) {
    Vector2 position = power_ups_pos.get(x);
    Vector2 info_position = new Vector2(info.position[0], info.position[1]);
    if (power_ups.size() > 0 && power_ups.get(x).getInfo().type.getTypeID() == info.type.getTypeID()
        && power_ups.get(x).getInfo().spawn_time == info.spawn_time
        && power_ups.get(x).getInfo().death_time == info.death_time && position.dst2(info_position) < 25) {
      return true;
    } else {
      return false;
    }
  }

  // Get next PowerUp
  public void determinePowerUp() {
    // System.out.println(handledPowerUps);
    for (int i = handledPowerUps; i < power_ups.size(); i++) {
      Vector2 position = power_ups_pos.get(i);
      if (power_ups_active.get(i) == true && !isBlocked((int) position.x, (int) position.y)
          && power_ups.get(i).getInfo().spawn_time - calcWalkTime(power_ups_pos.get(i)) < elapsed_time
          && (power_ups.get(i).getInfo().type.getTypeID() > 3 || getPowerUpCount() < 2)) {
        handledPowerUps = i;
        powerUp = power_ups_pos.get(i);
        // System.out.println("PowerUP "+ i+": "+ powerUp);
        return;
      } else {
        powerUp = null;
      }
    }
  }

  // Checks if powerUp has been picked up
  public void checkPowerUp() {
    for (int i = 0; i < 4; i++) { // go through all Enemies
      PlayerState enemy = GameManager.get().getPlayerState(i); // get PlayerState
      Vector2 player_pos = enemy.pos; // get Position
      if (player_pos != null) { // If its not null
        playerPositions[i] = player_pos; // write it in the player Positions
        player_pos.add(0.0f, -25.0f); // add some value
        for (int j = 0; j < power_ups.size(); j++) { // Go through all PowerUps
          // If they are active and spawned
          if (power_ups_active.get(j) == true && power_ups.get(j).getInfo().spawn_time < elapsed_time) {
            Vector2 buff_pos = new Vector2(power_ups_pos.get(j));
            double dist = buff_pos.sub(player_pos).len2();
            if (dist > 2500.0f)
              continue;
            // --- if the power up is collected, remove it from the board
            power_ups_active.set(j, false);
          }
        }
      }
    }
  }

  // Supposed to slightly overestimate calc time needed to walk to powerUp; never
  // worked
  // maybe you can get it to work :)
  public double calcWalkTime(Vector2 end) {
    // double length = me.pos.dst(end);
    // double updates = length/walkingSpeed;
    // double time = updates * GameManager.get().getDeltaTime();
    // time = time + GameManager.get().getDeltaTime();
    int time = 4;
    // System.out.println("Time " + time);
    return time;
  }

  public void checkPowerUpDeath() {
    for (int i = 0; i < power_ups.size(); i++) {
      if (power_ups_active.get(i) == true && power_ups.get(i).getInfo().death_time < elapsed_time) {
        power_ups_active.set(i, false);
      }
    }
  }

  public Vector2 findClosePaintStation() {
    Vector2 close = new Vector2(-2000, -2000);
    Vector2 extend = new Vector2(0, 0);
    boolean found = false;

    // ----General Paintstation----
    if (paintstation.size() > 5) { // If all paintstation locations available
      for (Vector2 paintst : paintstation) {
        if (me.pos.dst2(paintst) < me.pos.dst2(close)) {
          found = true;
          close = paintst;
        }
      }
      extend = new Vector2(close);
      extend.sub(me.pos);
      extend.setLength(10);
    } else { // If handselected paintstation
      if (paintstation.size() > 0) {
        // System.out.println(paintstation.size());
        Vector2 paintst = paintstation.get(paintstation.size() / 2);
        if (me.pos.dst2(paintst) < me.pos.dst2(close)) {
          found = true;
          close = paintst;
        }
        extend = new Vector2(0, 0);
      }
    }

    // ----Colored Paintstation----
    if (colored_paintstation.size() > 5) { // If all colored paintstation locations available
      for (Vector2 paintst : colored_paintstation) {
        if (me.pos.dst2(paintst) < me.pos.dst2(close)) {
          found = true;
          close = paintst;
        }
      }
      extend = new Vector2(close);
      extend.sub(me.pos);
      extend.setLength(10);
    } else { // If handselected colored paintstation
      if (colored_paintstation.size() > 0) {
        Vector2 paintst = colored_paintstation.get(colored_paintstation.size() / 2);
        if (me.pos.dst2(paintst) < me.pos.dst2(close)) {
          found = true;
          close = paintst;
        }
        extend = new Vector2(0, 0);
      }
    }
    close.add(extend); // close is extended into the paintstation; may overshoot
    if (found == false) {
      findRandomGoal();
      dontRecharge = true;
      recharge = false;
      close = goal;
    }
    // System.out.println("Paintstation: " + close);
    return close;
  }

  public boolean needsRecharge() {
    if (!dontRecharge && me.paint_amount / (float) me.max_paint_amount < 0.15f) {
      x = paintradius - 3; // paintradius -> needs to be smaller bc free spaces in round corners
      subDir = 0;
      mode = false;
      recharge = true;
      reachedGoalRecharge = false;
      return true;
    } else {
      return false;
    }
  }

  public void roomba() {
    // long starttime = System.nanoTime();
    if (!mode) {
      roombaPos = me.pos;
      mode = true;
    } else {
      if (me.pos.dst2(roombaPos) > x * x) {
        x += paintradius - 3;
        switch (subDir) {
          case 0:
            dir.setAngleDeg(0);
            subDir = 1;
            break;
          case 1:
            dir.setAngleDeg(90);
            subDir = 2;
            break;
          case 2:
            dir.setAngleDeg(-180);
            subDir = -1;
            break;
          case -1:
            dir.setAngleDeg(-90);
            subDir = 0;
            break;
          default:
            break;
        }
        mode = false;
      }
    }

    // int output = (int) (System.nanoTime() - starttime);
    // System.out.println("Roomba-Time: " + output);
  }

  public void stuckTest() {
    // TODO: increase panic stuck alarm
    if (panicStuckAlarm == 10) {
      panicStuck = me.pos;
    } else if (panicStuckAlarm < 0) {
      if (panicStuck.dst2(me.pos) < 20f) {
        // System.out.println("Panic stuck.");

        validPath = false;
        powerUp = null;
        walkToPowerUp = false;
        recharge = false;
        // power_ups_active.set(handledPowerUps, false);
        // goal=null;
        resetRecharge();

        if (recharge) {
          goal = findClosePaintStation();
        } else {
          findRandomGoal();
        }
        // System.out.println("Panic Stuck Goal: " + goal);

        subDir = 0;
        mode = false;
      }
      panicStuckAlarm = 11;
    }
    panicStuckAlarm--;
  }

  public void stuckTest2() {
    if (stuckAlarm == 3) {
      stuck = me.pos;
    } else if (stuckAlarm < 0) {
      if ((rawWalkingSpeed >= 0 && rawWalkingSpeed < 2.0f)) {
        // System.out.println("Am Stuck.");
        isStuck = true;
        validPath = false;
        if (recharge) {
          goal = findClosePaintStation();
        } else {
          findRandomGoal();
        }
        // System.out.println("Stuck Goal: " + goal);
        x = paintradius - 3; // paintradius -> needs to be smaller bc free spaces in round corners
        subDir = 0;
        mode = false;
        walkToPowerUp = false;
      }
      stuckAlarm = 4;
    }
    stuckAlarm--;
  }

  public void resetRecharge() {
    if (me.paint_amount / (float) me.max_paint_amount > 0.99f) {
      recharge = false;
      rechargeGoal = true;
      reachedGoalRecharge = false;
    }
  }
}