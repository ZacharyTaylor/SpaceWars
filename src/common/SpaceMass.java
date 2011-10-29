package common;

import java.io.Serializable;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**
 * The basic entity on which all objects in the game universe are based each
 * SpaceMass has a location, velocity, alive status and gravity_constant
 * @author kjb146 and zjt14 (largely based on code by Dr. Allan McInnes)
 */
public abstract class SpaceMass implements Serializable{

    //a unique id by which spaceMasses may be destingished
    public final int id = this.hashCode();

    // Default values for position and velocity vectors and heading
    private static final double DEFAULT_HEADING = Math.toRadians(0.0);
    private static final double DEFAULT_G = 0.0;

    // Current velocity in the game-space
    private Vector2d velocity = new Vector2d();

    // Current location in galaxy
    private Point2d location = new Point2d();

    // Radius of craft (used in hit detection)
    private int radius;

    // Current heading angle in radians
    private double angle = DEFAULT_HEADING;

    // "Gravity constant" of this object. Roughly analogous to GM, where
    // G is the newtonian gravity constant and M is the object mass.
    private double gravity_constant = DEFAULT_G;

    // True if the object is still active
    private boolean alive = true;
    
    /**
     * Creates an SpaceMass at the specified position, velocity and radius
     * @param radius the SpaceMass radius
     * @param initPos the SpaceMass position
     * @param initV the SpaceMass velocity
     */
    public SpaceMass(int radius, Point2d initPos, Vector2d initV) {
        location.set(initPos);
        velocity.set(initV);
        this.radius = radius;
    }

    /**
     * Updates the position based on the current velocity.
     */
    public void stepTime() {
        
        Point2d moveTo = getLocation();
        moveTo.add(velocity);
        setLocation(moveTo); // Assumes uniform timestep
    }

    /**
     * Modifies the object velocity by some vector amount
     * @param deltaV the change in velocity
     */
    public void accelerate(Vector2d deltaV) {
        velocity.add(deltaV);       
    }

    /**
     * Modifies the object velocity by the specified amount along the
     * line of the current object heading angle.
     * @param magnitude size of the change in velocity
     */
    public void accelerate(double magnitude) {
        velocity.add(new Vector2d(magnitude*Math.cos(angle),
                                  magnitude*Math.sin(angle)));
    }
   
      
    /**
     * Sets the position to the specified location relative to 0,0.
     * The position is specified as a Cartesian vector, and is internally
     * converted into location in the game-space
     * @param newPos vector specifying the new position
     */
    public void setLocation(Point2d newPos) {
        
        if((newPos.x + radius) > Galaxy.SIZE.width){
            newPos.x = radius;
        }
        if((newPos.y + radius) > Galaxy.SIZE.height){
            newPos.y = radius;
        }
        if((newPos.x - radius) < 0){
            newPos.x = Galaxy.SIZE.width - radius;
        }
        if((newPos.y - radius) < 0){
            newPos.y = Galaxy.SIZE.height - radius;
        }
        
        location = newPos;
    }

    /**
     * Rotates the object heading by the specified angle. A positive
     * rotation is in the clockwise direction.
     * @param turnAngle change in angle, in radians
     */
    public void rotate(double turnAngle) {
        angle = angleWraparound(angle + turnAngle);
    }

    /**
     * Applies mutual gravitational forces between this SpaceMass and
     * another one. Gravitational accelerations on each object
     * are computed using the standard Newtonian inverse-square law:
     *
     *   a = GM/r^2
     *
     * where GM is Newton's gravity constant multiplied by the mass of
     * the other object, and r is the distance between the objects.
     *
     * @param other the other object
     */
    public void gravitate(SpaceMass other) {
        // Find the spatial vector between this and the other object
        Vector2d grav_vector = new Vector2d(this.getLocation());
        grav_vector.sub(other.getLocation());

        // Compute the acceleration magnitudes for each object as a
        // function of gravity constant (which is sort of a proxy
        // for relative mass).
        double r2 = grav_vector.lengthSquared();
        double my_accel = -(other.getGravityConstant()/r2);
        double other_accel = this.getGravityConstant()/r2;

        // Convert the gravity vector into a pure direction, and then
        // scale to produce accelerations
        grav_vector.normalize();
        Vector2d my_accel_vector = new Vector2d(grav_vector);
        my_accel_vector.scale(my_accel);
        accelerate(my_accel_vector);

        Vector2d other_accel_vector = new Vector2d(grav_vector);
        other_accel_vector.scale(other_accel);
        other.accelerate(other_accel_vector);
    }

    /**
     * Makes the SpaceMass inactive.
     */
    public void destroy() {
        alive = false;
    }

    /**
     * Makes the SpaceMass move towards an inactive state, or become inactive
     * if it cannot sustain any more damage.
     */
    public void damage() {
        this.destroy();
    }

    /**
     * @return true if the SpaceMass is no longer active
     */
    public boolean isDead() {
        return !alive;
    }

    /**
     * Utility method used to keep angles within the range 0 <= angle <= 2*PI
     * @param angle an angle that may fall outside the designated range
     * @return an angle equivalent to the input, but within the
     * range 0 <= angle <= 2*PI
     */
    private static double angleWraparound(double angle) {
        double theta = angle;
        while (theta < 0.0) {
            theta += 2.0*Math.PI;
        }
        while (theta > 2.0*Math.PI) {
            theta -= 2.0*Math.PI;
        }
        return theta;
    }
    
    /**
     * @return returns the unique ID of this SpaceMass
     */
    public int getID(){
        return id;
    }
    
    /**
     * @return the radius of this SpaceMass 
     */
    public int getRadius(){
        return radius;
    }
    
    /**
     * @return the current velocity as a Cartesian vector
     */
    public Point2d getLocation() {
        return new Point2d(location);
    }

    /**
     * Sets the velocity to the specified Cartesian vector.
     * @param newV vector specifying the new velocity
     */
    public void setVelocity(Vector2d newV) {
        velocity = new Vector2d(newV);
    }

    /**
     * @return the current velocity as a Cartesian vector
     */
    public Vector2d getVelocity() {
        return new Vector2d(velocity);
    }
    
    /**
     * Sets the direction the SpaceMass is facing
     * @param angle the angle to were the SpaceMass is facing in radians
     */
    public void setHeading(double angle) {
        this.angle = angle;
    }
    
    /**
     * @return the current heading angle in radians
     */
    public double getHeading() {
        return angle;
    }
    
    /**
     * @return the current gravity constant
     */
    public double getGravityConstant() {
        return gravity_constant;
    }

    /**
     * Sets the gravity constant to a new value
     * @param G new gravity constant value
     */
    public void setGravityConstant(double G) {
        gravity_constant = G;
    }
}
