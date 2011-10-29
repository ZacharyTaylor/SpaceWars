package Server;

import common.Galaxy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import Server.PlayerBinding.HyperspaceListener;
import java.util.Random;

/**
 * GalaxyManager
 * Manages a single galaxy, updating of state, and clients
 * Loosely follows the Factory pattern
 * Can not be directly constructed, but can be requested and given a specific
 * manager dependent on criteria
 * @author kjb146 and zjt14
 */
public class GalaxyManager implements HyperspaceListener {

    //"Constants" for this manager
    private static final int UPDATE_PERIOD = 50; //update period in milliseconds (Period = 1000/frequency)
    private static final int MAX_CLIENTS_PER_GALAXY = 4; //Upper limit on clients per single galaxy
    private final Timer frameClock = new Timer();
    private final Galaxy galaxy = new Galaxy();
    private final Collection<PlayerBinding> players = Collections.synchronizedList(new ArrayList<PlayerBinding>());
// <editor-fold defaultstate="collapsed" desc="Static Methods for Galaxy Management">
    //All galaxies in game
    private static final Collection<GalaxyManager> managers = Collections.synchronizedList(new ArrayList<GalaxyManager>());

    /**
     * Adds the manager to the running environment
     * @param gm manager to add
     */
    private static void addManager(GalaxyManager gm) {
        gm.startManager();
        managers.add(gm);
    }

    /**
     * Removes a manager from the environment
     * @param gm manager to remove
     */
    private static void removeManager(GalaxyManager gm) {
        gm.kill();
        managers.remove(gm);
    }

    /**
     * Only public way to get a galaxy manager
     * @return a galaxyManager
     */
    public static GalaxyManager getManager() {
        GalaxyManager gm = new GalaxyManager();
        addManager(gm);
        return gm;
    }

    /**
     * Gets a random galaxy excluding the passed parameter
     * Used for finding galaxies to hyperspace to.
     * @param current Galaxy to ignore
     * @return a random galaxy except the passed argument
     */
    private static GalaxyManager getManagerNot(GalaxyManager current) {
        GalaxyManager gm;
        if (managers.size() > 1) {
            ArrayList<GalaxyManager> tempManagers = new ArrayList(managers);

            tempManagers.remove(current);
            int index = new Random().nextInt(tempManagers.size());

            gm = tempManagers.get(index);

            if (gm.players.size() >= MAX_CLIENTS_PER_GALAXY) {
                gm = getManager();
            }
        }else{
            gm = getManager();
        }
        return gm;
    }

    /**
     * Destroys ALL existing managers, and cleans up resources associated with them
     */
    public static void killAll() {
        Logger.getLogger("Server").log(Level.INFO, "KILLING ALL GALAXY MANAGERS");
        Collection<GalaxyManager> gmClone = new ArrayList<GalaxyManager>(managers);
        for (GalaxyManager gm : gmClone) {
            removeManager(gm);
        }
    }

    // </editor-fold>
    /**
     * Private constructor, to block public construction
     * (Not to be confused with Antidisestablishmentarianism)
     */
    private GalaxyManager() {
    }

    /**
     * Kills this manager, freeing any resources bound to it
     */
    private void kill() {
        for (PlayerBinding pb : players) {
            pb.setHyperspaceListener(null);
            pb.disconnect();
        }
        players.clear();
        frameClock.cancel();
        Logger.getLogger("Server").log(Level.INFO, "Galaxy Updater stopped");
    }

    /**
     * Schedules the galaxy to update a rate of "UPDATE_PERIOD"
     */
    private void startManager() {
        Logger.getLogger("Server").log(Level.INFO, "Galaxy Updater started");
        //Leave a period delay to ensure buffers loaded
        frameClock.scheduleAtFixedRate(frameSender, UPDATE_PERIOD, UPDATE_PERIOD);
    }

    /**
     * Adds a playerbinding to this manager / galaxy
     * @param pb player to add
     */
    public void addPlayer(PlayerBinding pb) {
        players.add(pb);
        pb.setGalaxy(galaxy);
        pb.setHyperspaceListener(this);
        Logger.getLogger("Server").log(Level.INFO, "Client has been added");
    }

    /**
     * Removes a playerbinding from this manager
     * @param pb
     */
    private void removePlayer(PlayerBinding pb) {
        players.remove(pb);
        Logger.getLogger("Server").log(Level.INFO, "Client has been removed");
        if (players.isEmpty()) {
            removeManager(this);
            Logger.getLogger("Server").log(Level.INFO, "Galaxy has been killed");
        }

    }
    //TimerTask responsible for updating the galaxy and the connected clients
    private TimerTask frameSender = new TimerTask() {

        @Override
        public void run() {

            //Update galaxy state
            galaxy.update();

            //Use clone so removes can be done in loop
            Collection<PlayerBinding> playersClone = new ArrayList<PlayerBinding>(players);
            for (PlayerBinding pb : playersClone) {
                try {
                    //Synchronized to avoid sending galaxies when clients are hyperspacing
                    synchronized (galaxy) {
                        pb.sendGalaxy();
                    }
                } catch (IOException ex) {
                    //On clients' socket closed
                    Logger.getLogger("Server").log(Level.INFO, "Removing Player");
                    removePlayer(pb);
                }
            }
        }

        ;
    };

    /**
     * Implementing interface HyperspaceListener
     * Used to physically move clients between galaxies
     * @param pb Client to move to new galaxy
     */
    @Override
    public void onHyperspace(PlayerBinding pb) {
        //Synchronized to avoid hyperspacing clients while sending galaxies.
        synchronized (galaxy) {
            GalaxyManager gm = GalaxyManager.getManagerNot(this);
            removePlayer(pb);
            gm.addPlayer(pb);
        }
    }
}
