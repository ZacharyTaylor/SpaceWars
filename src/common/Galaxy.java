package common;

import java.awt.Color;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import javax.vecmath.Point2d;

/**
 * an arena in the game. Holds a reference to all SpaceMasses within it. It also
 * determines how the masses within it move and interact. This class is the
 * only communication from the server to the client
 * @author kjb146 and zjt14
 */
public class Galaxy implements Serializable {

    //Default size 1024x768
    public static final Dimension SIZE = new Dimension(1024, 768);
    
    //The maxumum number of stars a galaxy can have
    private static final int MAX_STARS = 4;
    //The minimum size of a star
    private static final int MIN_STAR_SIZE = 20;
    //The range of star sizes
    private static final int STAR_SIZE_RANGE = 50;
    //Number of times to try find a safe place to spawn new objects
    private static final int SPAWN_ATTEMPTS = 100;
    
    // Tracks all allSpaceMasses currently in the galaxy.
    private Collection<SpaceMass> allSpaceMasses =
            Collections.synchronizedList(new ArrayList<SpaceMass>());
    //a random bright color for the stars in the galaxy to be
    private final Color starColor =
            new Color(Color.HSBtoRGB(new Random().nextFloat(), 1, 1));
    //assigns a random background to the galaxy
    private final int backNum = new Random().nextInt(7);
    //This is a reference for the end user of who is using the galaxy.
    private int playerID = 0;

    

    /**
     * Constructor for the galaxy spawns up to 5 suns at random locations
     */
    public Galaxy() {
        //number of stars
        int stars = new Random().nextInt(MAX_STARS + 1);

        //places stars of random size in galaxy
        for (int i = 0; i < stars; i++) {
            int radius = new Random().nextInt(STAR_SIZE_RANGE) + MIN_STAR_SIZE;
            
            //ensures stars are not colliding
            Point2d spawnPoint = safeSpawn(radius);
            
            addSpaceMass(new Star(radius, spawnPoint));
        }
    }

    /**
     * Respond to a timer event by updating the game state.
     */
    public synchronized void update() {
        //moves all the objects
        stepTime();
        //checks and deals with any collisions
        processCollisions();
    }

    /**
     * adds a spaceMass to the galaxy
     * @param spaceMass the spaceMass to add
     */
    public synchronized void addSpaceMass(SpaceMass spaceMass) {
        allSpaceMasses.add(spaceMass);
    }

    /**
     * removes a spaceMass from the galaxy
     * @param spaceMass the spaceMass to remove
     */
    public synchronized void removeSpaceMass(SpaceMass spaceMass) {
        allSpaceMasses.remove(spaceMass);
    }

    /**
     * provides a collection of all the spaceMasses in the galaxy
     * @return a collection of all the spaceMasses in the galaxy
     */
    public synchronized Collection<SpaceMass> getAllSpaceMasses() {
        return allSpaceMasses;
    }

    /**
     * finds a random safe location for a ship to spawn
     * @return the safe spawn location
     */
    public synchronized Point2d safeSpawn(int radius) {

        Random randGen = new Random();
        Point2d spawnPoint = new Point2d();

        //loop until safe location found or it fails 100 times in a row
        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            //find a random point
            spawnPoint = new Point2d(randGen.nextInt(SIZE.width),
                    randGen.nextInt(SIZE.height));

            //see if its safe
            boolean safe = true;

            for (SpaceMass sm : allSpaceMasses) {
                double distance = sm.getLocation().distance(spawnPoint);
                double minDist = sm.getRadius() + radius;

                if (distance < minDist) {
                    safe = false;
                }
            }
            if (safe) {
                return spawnPoint;
            }

        }

        return spawnPoint;
    }

    /**
     * Detects and deals with any collisions. Assumes all objects are circles
     */
    private void processCollisions() {
        //temp collection created to prevent objects being itterated ovr twice
        Collection<SpaceMass> temp = new ArrayList<SpaceMass>(allSpaceMasses);

        //for each SpaceMass in galaxy
        for (SpaceMass sm1 : allSpaceMasses) {
            //removes SpaceMass to prevent double ups
            temp.remove(sm1);

            //check against rest of SpaceMasses
            for (SpaceMass sm2 : temp) {
                double distance = sm1.getLocation().distance(sm2.getLocation());
                double radius = sm1.getRadius() + sm2.getRadius();

                //if circles of SpaceMasses intersect damage them
                if ((distance < radius) && !sm1.equals(sm2)) {
                    sm1.damage();
                    sm2.damage();
                }
            }
        }
    }

    /**
     * On call progresses time in galaxy by one "step"
     */
    private void stepTime() {

        for (SpaceMass mass1 : allSpaceMasses) {
            for (SpaceMass mass2 : allSpaceMasses) {
                if (mass1 == mass2) {
                    continue; //Dont gravitate to self (not really required)
                }
                mass1.gravitate(mass2);
            }
        }

        // Update positions and mark all dead objects
        ArrayList<SpaceMass> deadObjects = new ArrayList<SpaceMass>();
        for (SpaceMass sm : allSpaceMasses) {
            sm.stepTime();
            if (sm.isDead()) {
                deadObjects.add(sm);
            }
        }

        // Remove dead objects
        for (SpaceMass sm : deadObjects) {
            if (sm instanceof Spacecraft) {
                Debris tempDebris =
                        new Debris(sm.getLocation(), sm.getVelocity(), sm.id);
                allSpaceMasses.add(tempDebris);
            }
            removeSpaceMass(sm);
        }
    }

    /**
     * Sets the player id so the client knows who their ship is
     * @param playerID the id of the players ship
     */
    public synchronized void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    /**
     * @return the ID of the players ship 
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * @return the color of stars in this galaxy 
     */
    public synchronized Color getStarColor() {
        return starColor;
    }

    /**
     * @return the number of the background to use for this galaxy
     */
    public int getBackNum() {
        return backNum;
    }
}
