package common;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**
 * A star is a large gravitating object that spins. Stars cannot be
 * damaged, destroyed, or moved.
 * @author kjb146 and zjt14 (largely based on code by Dr. Allan McInnes)
 */
public class Star extends SpaceMass {

    /**
     * A star is a large SpaceMass that spins. Its gravity constant is
     * proportional to its radius cubed
     * @param radius the stars radius
     * @param pos the stars position in the galaxy
     */   
    public Star(int radius, Point2d pos) {
        super(radius, new Point2d(pos), new Vector2d(0.0, 0.0));
        int G = (int)(0.01*Math.pow(radius, 3));
        this.setGravityConstant(G);
    }

    /**
     * stars cannot be destroyed
     */
    @Override
    public void destroy() {
        return;
    }
    
    /**
     * stars cannot be damaged
     */
    @Override
    public void damage() {
        return;
    }
    
    /**
     * stars revolve but otherwise do not move
     */
    @Override
    public void stepTime() {
        // Even though ships are defined as having effectively zero mass, 
        // we *ensure* that a star remains fixed by disabling position
        // updates. This avoids any problems with numerical rounding
        // errors. A more realistic model might allow stars to influence
        // each other. This is easily achieved by inserting a call to
        // super.stepTime() here.
        
        this.rotate(Math.PI/40.0);
    }    
}

