package common;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**
 * A spacecraft is the game player's representative in the game space.
 * Spacecraft are able to maneuver and shoot at each other.
 * @author kjb146 and zjt14 (largely based on code by Dr. Allan McInnes)
 */
public class Spacecraft extends SpaceMass {

    //constants
    
    //Size of spacecraft
    public static final int SPACECRAFT_R = 40;
    //Maximum velocity of the Spacecraft
    private static final double MAX_VELOCITY = 20;
    //Period of firerate (Milliseconds)
    private static final int FIRE_RATE = 200;
    //Period of hyperspace (Milliseconds)
    private static final int HYPERSPACE_RATE = 1000;
    // delta-V provided by one thrust command
    private static final double IMPULSE = 2.0;
    // Change in orientation provided by one rotate command
    private static final double TURN_INCREMENT = 0.2;
    // Number of hits the spacecraft can take
    private static final int DEFAULT_SHIELDS = 5;
    // Fuel spacecraft has to burn manovering
    private static final int DEFAULT_FUEL = 2000;
    // Number of missiles left to shoot
    private static final int DEFAULT_MISSLE_COUNT = 50;
    // Number of missiles left to shoot
    private static final Vector2d DEFAULT_SPEED = new Vector2d(0,0);
    // Number of missiles left to shoot
    private static final Point2d DEFAULT_POSITION = new Point2d(0,0);
    
    
    //Time since Missile last fired
    private long coolDownMissile = System.currentTimeMillis();
    //Time since last hyperspace jump
    private long coolDownHyperspace = System.currentTimeMillis();
    // Number of hits the spacecraft can take
    private int shields;
    // Fuel spacecraft has to burn manovering
    private int fuel;
    // Number of missiles left to shoot
    private int missilesLeft;

    /**
     * Constructor creates a Spacecraft with default speed and position
     */
    public Spacecraft(){
        super(SPACECRAFT_R, DEFAULT_POSITION, DEFAULT_SPEED);
        reset();
    }

    /**
     * Resets the shields, fuel, missiles, speed and location to the 
     * default starting values
     */
    public final void reset(){
        shields = DEFAULT_SHIELDS;
        fuel = DEFAULT_FUEL;
        missilesLeft = DEFAULT_MISSLE_COUNT;
        setVelocity(DEFAULT_SPEED);
        setLocation(DEFAULT_POSITION );
    }

    /**
     * @return true if the ships shields are at <= 0, false otherwise
     */
    @Override
    public boolean isDead(){
        return (shields<=0);
    }

    /** 
     * Damaging a spacecraft reduces its shield levels. When shield levels 
     * reach 0 the spacecraft is destroyed.
     */
    @Override
    public void damage() {
        --shields;
        if (shields < 1) {
            this.destroy();
        }
    }

    /**
     * Fire the spacecraft thrusters to provide a change in velocity (delta-V).
     * Consumes 10 fuel
     */
    public void thrust() {
        if (fuel >= 10) {
            accelerate(IMPULSE);

            //Cap speed
            if (this.getVelocity().length() > MAX_VELOCITY) {
                accelerate(-IMPULSE);
            } else {
                fuel -= 10;
            }


        }
    }

    /**
     * Rotate the spacecraft clockwise. Consumes 1 fuel
     */
    public void clockwise() {
        if (fuel > 0) {
            rotate(-TURN_INCREMENT);
            fuel--;
        }
    }

    /**
     * Rotate the spacecraft counter-clockwise. Consumes 1 fuel
     */
    public void counterClockwise() {
        if (fuel > 0) {
            rotate(TURN_INCREMENT);
            fuel--;
        }
    }

    /** 
     * Launch a missile
     * @param galaxy 
     */
    public void fire(Galaxy galaxy) {
        //if cooldown has expired
        if (((System.currentTimeMillis() - coolDownMissile) > FIRE_RATE)
                && (missilesLeft > 0)) {
            
            coolDownMissile = System.currentTimeMillis();
            
            galaxy.addSpaceMass(new Missile(
                    this.getLocation(),
                    new Vector2d(0, 0),
                    this.getHeading()));
            
            missilesLeft--;
        }
    }

    /**
     * jumps the spacecraft into a random galaxy. Consumes 100 fuel
     * @return true if sufficient fuel to jump, false otherwise
     */
    public boolean hyperspace() {
        boolean rtn = false;
        //if cooldown has expired
        if (((System.currentTimeMillis() - coolDownHyperspace) > HYPERSPACE_RATE)
                && (fuel >= 100)) {
            coolDownHyperspace = System.currentTimeMillis();
            fuel -= 100;
            rtn = true;
        }
        return rtn;
    }

    /**
     * @return fuel left
     */
    public int getFuel() {
        return fuel;
    }

    /**
     * @return Shield strength of spacecraft
     */
    public int getShields() {
        return shields;
    }

    /*
     * @return number of missiles left in Spacecraft
     */
    public int getMissilesLeft() {
        return missilesLeft;
    }
}
