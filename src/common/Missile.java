package common;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**
 * A missile is a weapon fired by a spacecraft. Despite the name, it's actually
 * a ballistic projectile once its thruster has fired to provide an 
 * initial acceleration. Missiles don't arm until the reach a predefined
 * distance from the launching spacecraft, to prevent them from killing their
 * launcher. Each missile has a finite lifetime, after which it destroys itself.
 */
public class Missile extends SpaceMass {
    
    // The delta-V provided by the missile thruster when it fires                                           
    private static final double IMPULSE = 10.0;
    
    // The distance from the spacecraft the missile has to be before it becomes
    // active
    private static final double IGNITION_DISTANCE = 40.0;
    
    // The number of time-steps the missile will stay active. 
    private static final int INIT_LIFE = 1000;
    
    // Remaining number of time-steps before the missile becomes inactive.
    private int lifetime = INIT_LIFE;

    /**
     * Launch a missile from a spacecraft with the specified position and
     * heading. The missile will start near the spacecraft, and have a velocity
     * that is the vector sum of the current spacecraft velocity and the 
     * launch delta-V of the missile applied in the direction the spacecraft
     * is currently pointed.
     *
     * @param initPos spacecraft position
     * @param initV spacecraft velocity
     * @param heading spacecraft orientation
     */
    public Missile(Point2d initPos, Vector2d initV, double heading) {
        super(5, initPos, initV);

        // Move the missile position to the launch distance
        initPos.add(new Point2d(IGNITION_DISTANCE * Math.cos(heading),
                IGNITION_DISTANCE * Math.sin(heading)));
        this.setLocation(initPos);

        this.rotate(heading);

        // Determine the missile velocity after launch
        Vector2d launch_accel = new Vector2d(
                IMPULSE * Math.cos(heading),
                IMPULSE * Math.sin(heading));
        this.accelerate(launch_accel);
        
        super.stepTime(); // Force a position update
    }

    /**
     * Update the missile position and velocity, and check to see if the
     * missile has exceeded its lifetime.
     */
    @Override
    public void stepTime() {
        super.stepTime();
        //Set heading to current velocity direction W.R.T. due north
        double heading = Math.atan2(
                this.getVelocity().y,
                this.getVelocity().x);
        
        this.setHeading(heading);


        --lifetime;
        if (lifetime <= 0) {
            this.destroy();
        }
    }
}
