package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a game server, it is responsible for starting and stopping
 * the server process, and can run as a standalone server.
 * @author kjb146 and zjt14
 */
public class Server {
    //Socket configuration

    public final static InetAddress MULTICAST_ADDRESS = getAddress();
    public final static int MULTICAST_PORT = 4280;
    public final static int TCP_PORT = 4281;
    public final static int SOCKET_BUFFER_SIZE = 255;
    //Control Messages
    public final static String MSG_SERVER_REQUEST = "HELO_SERVER?";
    public final static String MSG_CONNECT_REQUEST = "BEAM_ME_UP ";

    //Private variables
    private volatile MulticastSocket mSocket;
    //Name of the server
    private String name = "DEFAULT SERVER";

    /**
     * gets the InetAddress for the multicast configuration
     * @return the multicast address object
     */
    private static InetAddress getAddress() {
        InetAddress rtn = null;
        try {
            rtn = InetAddress.getByName("228.4.2.8");
        } catch (UnknownHostException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rtn;
    }

    /**
     * Testing run target. starts up a server which runs until force close by user
     * @param args (unused)
     */
    public static void main(String args[]) {
        Server s = new Server();
        try {
            Logger.getLogger("Server").log(Level.INFO, "Server Started");
            s.startServer();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Constructor, doesn't do anything special
     */
    public Server() {
        Logger.getLogger("Server").setLevel(Level.SEVERE);
    }

    /**
     * Tests if the server is running (StartServer has been called, and server is still running)
     * @return true if server running, false otherwise
     */
    public boolean isRunning() {
        return (mSocket != null && !mSocket.isClosed());
    }

    /**
     * Starts the server process.
     * Non-blocking call, leaves a server in a separate thread.
     */
    public void startServer() {
        try {
            mSocket = new MulticastSocket(MULTICAST_PORT);
            mSocket.joinGroup(MULTICAST_ADDRESS);
            new Thread(multicastListener).start();
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stops the server process and cleans up associated data.
     */
    public void stopServer() {
        GalaxyManager.killAll();
        try {
            mSocket.leaveGroup(MULTICAST_ADDRESS);
            mSocket.close();
        } catch (IOException ex) {
            Logger.getLogger("Server").log(Level.SEVERE, "Error stopping server");
        }
    }

    /**
     * Sets the user chosen name to identify the server. can be any ascii sequence
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * MulticastListener process.
     * This is the main loop for the server, listening on the multicast channel,
     * and responding according to the data it receives
     */
    private Runnable multicastListener = new Runnable() {

        @Override
        public void run() {

            //define the receive buffer and packet
            byte[] buffer = new byte[SOCKET_BUFFER_SIZE];
            DatagramPacket dgPacket = new DatagramPacket(buffer, buffer.length);

            try {
                //As try is outside while loop, on error it breaks the loop
                while (!mSocket.isClosed()) {

                    //RESET BUFFER
                    java.util.Arrays.fill(buffer, (byte) 0);

                    //Block until packet, then load it to dgPacket
                    mSocket.receive(dgPacket);

                    //extract important information from packet
                    String msg = new String(dgPacket.getData()).trim();
                    String ip = dgPacket.getAddress().getHostAddress();

                    //Parse possibilities.
                    if (msg.startsWith(MSG_SERVER_REQUEST)) {
                        //Send server name
                        DatagramPacket tx = new DatagramPacket(name.getBytes(), name.length(), MULTICAST_ADDRESS, MULTICAST_PORT);
                        mSocket.send(tx);
                    } else if (msg.startsWith(MSG_CONNECT_REQUEST)) {
                        //If target requested IP is mine
                        if (msg.substring(msg.indexOf(" ")).trim().equals(MULTICAST_ADDRESS.getLocalHost().getHostAddress())) {
                            addClient(ip);
                        }
                    }
                }
            } catch (IOException ex) {
            }
        }
    };

    /**
     * Adds a client to server's galaxyManager
     * @param ip <String> of the client to connect to
     */
    private void addClient(String ip) {
        try {
            Logger.getLogger("Server").log(Level.INFO, "Adding Client");

            Socket s = new Socket(ip, TCP_PORT);
            s.setTcpNoDelay(true); //Stops issues with packet delay
            PlayerBinding pb = new PlayerBinding(s);

            GalaxyManager gm = GalaxyManager.getManager();
            gm.addPlayer(pb);

        } catch (UnknownHostException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
