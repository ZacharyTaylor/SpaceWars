package Client;

import common.ClientInput;
import common.Galaxy;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import Server.Server;
import java.io.EOFException;
import java.net.InetAddress;
import javax.swing.JOptionPane;

/**
 * Manages the bindings between the network messages and the graphical representation
 * @author kjb146 and zjt14
 */
public class ClientManager {

    //Private variables (Volatile to avoid optimizing out concurrency)

    //Sockets
    private volatile MulticastSocket mSocket;
    private volatile ServerSocket sSocket;
    private volatile Socket socket;
    private volatile ObjectInputStream objIn = null;
    private volatile ObjectOutputStream objOut = null;

    //Client graphics and controls
    private volatile Client client;
    private ClientInput clientInput = new ClientInput();

    /**
     * Test run target. connects to the local machine for testing purposes
     * @param args (unused)
     */
    public static void main(String args[]) {
        ClientManager cm = new ClientManager();
        try {
            cm.connectTo(InetAddress.getLocalHost().getHostAddress(), 500);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Constructor, Initializes objects, but doesn't execute them
     */
    public ClientManager() {
        Logger.getLogger("Client").setLevel(Level.SEVERE);
        try {
            mSocket = new MulticastSocket(Server.MULTICAST_PORT);
            client = new Client();
            sSocket = new ServerSocket(Server.TCP_PORT);

        } catch (IOException ex) {
            Logger.getLogger("Client").log(Level.SEVERE, "Could not subscribe to port: {0}", Server.TCP_PORT);
            JOptionPane.showMessageDialog(null, "An instance of Spacewars! launcher is already running");
            System.exit(-1);
        }
    }

    /**
     * Forces the display to be visible. Useful when it has been minimized
     */
    public void forceShow() {
        client.setVisible(true);
    }

    /**
     * Polls the servers for their names (Blocking call)
     * @param timeout (milliseconds) how long to wait for a reply
     * @return a map of <IP,Name> (Stops a single IP from registering multiple server names)
     */
    public Map<String, String> getServerList(final int timeout) {
        final Map<String, String> serverMap = new HashMap<String, String>();

        Thread bgThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    //Define buffer and packet
                    byte[] buff = new byte[Server.SOCKET_BUFFER_SIZE];
                    DatagramPacket dgp = new DatagramPacket(buff, buff.length);

                    mSocket.joinGroup(Server.MULTICAST_ADDRESS);
                    mSocket.setSoTimeout(timeout);

                    long end_time = System.currentTimeMillis() + timeout;
                    while (System.currentTimeMillis() < end_time) {
                        //Flush Buffer
                        java.util.Arrays.fill(buff, (byte) 0);

                        mSocket.receive(dgp);
                        String payload = new String(dgp.getData());

                        //If message isn't a known type then assume it is a server name and add it to the list:
                        if (!(payload.startsWith(Server.MSG_CONNECT_REQUEST) || payload.startsWith(Server.MSG_SERVER_REQUEST))) {
                            serverMap.put(dgp.getAddress().getHostAddress(), payload);
                        }
                    }
                } catch (SocketTimeoutException ex) {
                    //This has broken while loop (good)
                } catch (IOException ex) {
                    //This also broke while loop (doesnt matter).
                } finally {
                    try {
                        mSocket.leaveGroup(Server.MULTICAST_ADDRESS);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        //Start multicast listening
        bgThread.start();

        //Request servers to send on multicast
        sendMulticast(Server.MSG_SERVER_REQUEST);

        //Wait for multicast listening to finish
        try {
            bgThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger("Client").log(Level.SEVERE, "Interupted trying to get ServerList");
        }

        return serverMap;
    }

    /**
     * Sends a message on the multicast channel
     * @param msg string to send
     */
    private void sendMulticast(String msg) {
        DatagramPacket dgp = new DatagramPacket(msg.getBytes(), msg.length(), Server.MULTICAST_ADDRESS, Server.MULTICAST_PORT);
        try {
            mSocket.send(dgp);
        } catch (IOException ex) {
            Logger.getLogger("Client").log(Level.SEVERE, "Could not send on multicast channel");
        }
    }

    /**
     * Connects to a given ip
     * @param ip server IP to request a connection from
     * @param timeout (milliseconds) time to wait for the server to connect
     */
    public void connectTo(final String ip, final int timeout) {
        Thread bgThread = new Thread(new Runnable() {

            @Override
            public void run() {
                waitAndConnect(timeout);
            }
        });

        bgThread.start();
        requestConnection(ip);
        try {
            bgThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger("Client").log(Level.SEVERE, "Thread got interupted");
        } finally {
            Logger.getLogger("Client").log(Level.INFO, "Killing ClientManager");
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Requests a connection to be made to client from given ip
     * @param ip Server ip to request connection from
     */
    private void requestConnection(String ip) {
        sendMulticast(Server.MSG_CONNECT_REQUEST + ip);
    }

    /**
     * Wait for the server to reverse connect to the client
     * @param timeout (milliseconds) maximum time to wait for connection
     */
    private void waitAndConnect(int timeout) {
        try {

            sSocket.setSoTimeout(timeout);
            socket = sSocket.accept();
            Logger.getLogger("Client").log(Level.INFO, "Connection found");
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objIn = new ObjectInputStream(socket.getInputStream());

            Logger.getLogger("Client").log(Level.INFO, "Streams Created");

            socket.setTcpNoDelay(true);

            client.setVisible(true);
            clientLoop();
            socket.close();

            try {
                //Add a bit of delay to elegantly hide window
                Thread.sleep(200);
                client.setVisible(false);
            } catch (InterruptedException ex) {}

            client.setVisible(false);
            
        } catch (SocketTimeoutException ex) {
            //This is ok, timeout is acceptable
        } catch (IOException ex) {
            Logger.getLogger("Client").log(Level.SEVERE, "Error opening socket");
            ex.printStackTrace();
        }
    }

    /**
     * Main loop run to update graphics and send keypushes to server
     */
    private void clientLoop() {
        try {
        
            while (!socket.isClosed()) {

             //   objIn.reset();
                Galaxy g = (Galaxy) objIn.readObject();
                client.update(g);

                objOut.reset();
                clientInput.poll(client.getKeyMapper());
                objOut.writeObject(clientInput);
            }

        } catch (EOFException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
            //Do nothing. socket close is the thread end flag
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tests if the client is connected to the server
     * @return true for connected, false otherwise
     */
    public boolean isRunning() {
        return (socket != null && socket.isConnected() && !socket.isClosed());
    }
}
