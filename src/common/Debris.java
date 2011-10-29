package common;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**
 * Debris are the remains of dead spacecraft. They respond to gravity while
 * spinning uncontrolled. They have health and a timeout
 * @author kjb146 and zjt14
 */
public class Debris extends SpaceMass {

    //defining constants
    
    //The number of frames in the explosion animation
    public final static int FRAMES_IN_EXPLOSION = 16;
    //The size of the explosion
    public final static Point2d EXPLOSION_SIZE = new Point2d(64,64);
    //area around the debris it can damage
    private static final int DEBRIS_RADIUS = 20;
    //the speed at whict the debris spin on the x axis
    private static final float DEBRIS_TUMBLE_SPEED = 0.1f;
    //the speed at whict the debris spin on the z axis
    private static final float DEBRIS_SPIN_SPEED = 0.2f;
    // The number of time-steps the debris will stay active.
    private static final int INIT_TIME = 1000;
    //the number of hits the debris can take
    private static final int INIT_HEALTH = 5;
    
    
    
    //3D spin of debris
    private float tumble = 0;

    //state of exploding debris is in
    private int explosion = 0;

    //the id of the ship that turned into the debris
    private int debrisID;

    //the health of the debris
    private int health = INIT_HEALTH;

    // Remaining number of time-steps before the debris becomes inactive.
    private int lifetime = INIT_TIME;

/**
     * Places the debris in the galaxy
     * @param pos the debris initial position
     * @param vel the debris initial velocity
     * @param id the id of the SpaceMass the debris were formed from
     */
    public Debris(Point2d pos, Vector2d vel, int id) {
        super(DEBRIS_RADIUS, pos, vel);
        debrisID = id;
    }

    /**
     * reduces the health of the debris. If the health = 0 it destroys them
     */
    @Override
    public void damage() {
        --health;
        if (health < 1) {
            this.destroy();
        }
    }

    /**
     * debris both move and tumble end over end
     */
    @Override
    public void stepTime() {
        super.stepTime();

        //tumble debris
        tumble += DEBRIS_TUMBLE_SPEED ;
        this.setHeading(this.getHeading() + DEBRIS_SPIN_SPEED );

        //prevents angle exceeding 2*Pi
        while(tumble > (2 * Math.PI)){
            tumble -= (2 * Math.PI);
        }

        //animates the explosion
        if(explosion <= FRAMES_IN_EXPLOSION){
            explosion++;
        }

        //limits duration of debris
        --lifetime;
        if (lifetime <= 0) {
            this.destroy();
        }
    }

    /**
     * @return debris 3D tumble
     */
    public float getTumble(){
        return tumble;
    }

    /**
     * @return id of the ship that was turned into debris
     */
    public int getDebrisID(){
        return debrisID;
    }

    /**
     * @return state of exploding debris is in
     */
    public int getExplosionFrame(){
        return explosion;
    }
}