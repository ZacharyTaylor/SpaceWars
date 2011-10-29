package Server;

import common.ClientInput;
import common.Galaxy;
import common.Spacecraft;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the join between a spacecraft and a client socket
 * It is responsible for the updating of the spacecraft from user input
 * @author kjb146 and zjt14
 */
public class PlayerBinding {

    /* Private variables (most volatile to avoid optimization
     * which can interfere with multiple threads accessing and changing them
     */
    private Spacecraft spacecraft;
    private volatile Galaxy galaxy;
    private HyperspaceListener hsListener;

    //Socket variables
    private volatile Socket socket;
    private volatile ObjectInputStream objIn;
    private volatile ObjectOutputStream objOut;
    
    /**
     * Constructor, generates associated resources for the given connection
     * @param socket the socket linked to the client
     */
    public PlayerBinding(Socket socket) {
        try {
            spacecraft = new Spacecraft();

            this.socket = socket;

            objOut = new ObjectOutputStream(socket.getOutputStream());
            //This is a blocking constructor
            objIn = new ObjectInputStream(socket.getInputStream());


            startConnection();

        } catch (IOException ex) {
            Logger.getLogger(PlayerBinding.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Assigns the given galaxy to the client (and ship)
     * Cleans up previous galaxy bindings
     * @param g galaxy to assign
     */
    public void setGalaxy(final Galaxy g) {
        if (galaxy != null) {
                galaxy.removeSpaceMass(spacecraft);
        }
       // synchronized (g) {
        galaxy = g;
        spacecraft.setLocation(g.safeSpawn(spacecraft.getRadius()));
        galaxy.addSpaceMass(spacecraft);
      //  }
    }

    /**
     * Starts the binding listening for client inputs, to update spacecraft state
     * NON-BLOCKING
     */
    private final void startConnection() {
        new Thread(inputListener).start();
    }

    /**
     * Assigns a hyperspace listener to the binding,
     * The listener is triggered when the craft wants to hyperspace
     * @param hl
     */
    public void setHyperspaceListener(HyperspaceListener hl) {
        hsListener = hl;
    }

    /**
     * Sends the galaxy state to the far client
     * @throws IOException when socket is closed or corrupt
     */
    public void sendGalaxy() throws IOException {
        //Check against null pointers
        if (galaxy == null) {
            return;
        }
        //synchronized (galaxy) {
            galaxy.setPlayerID(spacecraft.id);

            objOut.reset();
            objOut.writeObject(galaxy);
      //  }
    }

    //The listener for userinput and actions appropriately
    private Runnable inputListener = new Runnable() {

        @Override
        public void run() {
            try {

                while (!socket.isClosed()) {
                    ClientInput ci = (ClientInput) objIn.readObject();

                    /*Parse inputs*/

                    //rageQuit, regardless of player alive/dead
                    if (ci.getExit()) {
                        disconnect();
                    }
                    if (spacecraft.isDead()) {
                        //Only if client is dead
                        if (ci.getRestart()) {
                            Logger.getLogger("Server").log(Level.INFO, "Client attempted restart");
                            spacecraft.reset();
                            doHyperspace();
                        }
                    } else {
                        //Only if client is alive
                        if (ci.getFire()) {
                            spacecraft.fire(galaxy);
                        }
                        if (ci.getForward()) {
                            spacecraft.thrust();
                        }
                        if (ci.getLeft()) {
                            spacecraft.counterClockwise();
                        }
                        if (ci.getRight()) {
                            spacecraft.clockwise();
                        }
                        if (ci.getJump()) {
                            Logger.getLogger("Server").log(Level.INFO, "CLIENT TRIED HYPERSPACE");
                            doHyperspace();
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger("Server").log(Level.SEVERE, "network to object exception");
            } catch (IOException ex) {
                Logger.getLogger("Client").log(Level.SEVERE, "MSG: " + ex.getMessage());
                ex.printStackTrace();
            }

            //Once code has left block (on socket error) disconnect to clean up socket
            disconnect();
        }
    };

    /**
     * If hyperspace listener exists,
     * Request hyperspace, if craft allows it then call hyperspacelistener
     * (for higher level to implement the move)
     */
    private void doHyperspace() {
        if (hsListener != null && spacecraft.hyperspace()) {
            hsListener.onHyperspace(this);
        }
    }

    /**
     * Cleans up all resources associated with the socket
     */
    public void disconnect() {
        try {
            Logger.getLogger("Server").log(Level.INFO, "Client Disconnected");
            objIn.close();
            objOut.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger("Server").log(Level.INFO, "Player disconnected {0}", new String[]{socket.getInetAddress().getHostAddress()});
            ex.printStackTrace();
        }
    }

    /**
     * Interface HyperspaceListener
     * Provides a gateway for hyperspace to be implemented outside PlayerBinding
     */
    public interface HyperspaceListener {

        /**
         * Is called when a ship is to be hyperspaced
         * @param pb the playerbinding which requested the jump
         */
        public void onHyperspace(PlayerBinding pb);
    }
}
