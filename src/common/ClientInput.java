package common;

import com.threed.jpct.util.KeyMapper;
import com.threed.jpct.util.KeyState;
import java.awt.event.KeyEvent;
import java.io.Serializable;

/**
 * Detects and holds the clients key presses for sending to the server. Allows
 * multiple keys to be pressed simultaneously. Acts as the only communication 
 * from the client to the server
 * @author kjb146 and zjt14
 */
public class ClientInput implements Serializable {

    //The keys the user may be pressing
    private boolean left = false;
    private boolean right = false;
    private boolean forward = false;
    private boolean fire = false;
    private boolean jump = false;
    private boolean exit = false;
    private boolean restart = false;

    /**
     * Uses the KeyMapper to poll the keyboard detecting all key changes
     * @param keyMapper the keyMapper to poll (note passed in rather then
     * kept in ClientInput as it doesn't need to be sent to the server
     */
    public void poll(KeyMapper keyMapper) {
        KeyState state;
        do {
            //scans through each key to see if state has changed since that
            //key was last scanned. Returns on first difference detected with
            //state
            state = keyMapper.poll();

            //if a key has changed
            if (state != KeyState.NONE) {
                //change the value of the recorded key state
                keyAffected(state);
            }
            //keep going till all changed keys updated
        } while (state != KeyState.NONE);
    }

    /**
     * Scans to see if the key that has changed is one of those used by the
     * game and if it is updates its value
     * @param state the state of the changed key
     */
    private void keyAffected(KeyState state) {

        //gets the code of the pressed key
        int code = state.getKeyCode();

        //gets if the key is down or up
        boolean event = state.getState();

        //checks to see if the key is used by the game and if it is updates
        //its value
        switch (code) {
            case (KeyEvent.VK_LEFT): {
                left = event;
                break;
            }
            case (KeyEvent.VK_RIGHT): {
                right = event;
                break;
            }
            case (KeyEvent.VK_UP): {
                forward = event;
                break;
            }
            case (KeyEvent.VK_SPACE): {
                fire = event;
                break;
            }
            case (KeyEvent.VK_M): {
                jump = event;
                break;
            }
            case (KeyEvent.VK_ESCAPE): {
                exit = event;
                break;
            }
            case (KeyEvent.VK_ENTER): {
                restart = event;
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * @return the state of the key bound to turn left, true == pressed
     */
    public boolean getLeft() {
        return left;
    }

    /**
     * @return the state of the key bound to turn right, true == pressed
     */
    public boolean getRight() {
        return right;
    }

    /**
     * @return the state of the key bound to fire, true == pressed
     */
    public boolean getFire() {
        return fire;
    }

    /**
     * @return the state of the key bound to hyperspace jump, true == pressed
     */
    public boolean getJump() {
        return jump;
    }

    /**
     * @return the state of the key bound to exit, true == pressed
     */
    public boolean getExit() {
        return exit;
    }

    /**
     * @return the state of the key bound to forwards, true == pressed
     */
    public boolean getForward() {
        return forward;
    }

    /**
     * @return the state of the key bound to restart, true == pressed
     */
    public boolean getRestart() {
        return restart;
    }
}
