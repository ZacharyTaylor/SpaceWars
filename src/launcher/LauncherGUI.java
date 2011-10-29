package launcher;

import Client.ClientManager;
import Server.Server;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * GUI for starting servers and connecting to them
 * Used to simplify the process of starting the game
 * @author kjb146 and zjt14
 */
public class LauncherGUI {

    //List of JComponents used
    private JPanel mainPanel;
    private ServerList serverList;
    private ClientManager clientManager;
    private Server server;
    private JButton startServerButton, updateButton, connectButton;
    
    /*Time in milliseconds*/
    private static int SERVER_SEARCH_TIME = 100; //Time to wait for servers to respond with their names
    private static int SERVER_CONNECT_TIMEOUT = 1000; //Time to allow a server to reverse connect within

    /**
     * Bootstrap the gui to start
     * @param args (Unused)
     */
    public static void main(String args[]) {
        LauncherGUI app = new LauncherGUI();
    }

    /**
     * Constructor
     * Builds the gui and displays it
     */
    public LauncherGUI() {
        JFrame frame = new JFrame("SpaceWars Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(300, 400));
        frame.setMaximumSize(new Dimension(500, 600));
        //frame.setResizable(false);

        initComponents();
        layoutComponents();
        setBehaviours();

        clientManager = new ClientManager();
        server = new Server();

        frame.add(mainPanel);
        frame.pack();
        frame.setVisible(true);

        updateButton.doClick();
    }

    /**
     * Creates components, configures parameters, and adds to mainPanel
     */
    private void initComponents() {

        mainPanel = new JPanel();

        serverList = new ServerList();

        updateButton = new JButton();
        updateButton.setText("Update Server List");

        connectButton = new JButton();
        connectButton.setText("Connect");

        startServerButton = new JButton();
        startServerButton.setText("Start New Server");

        mainPanel.add(serverList);
        mainPanel.add(updateButton);
        mainPanel.add(connectButton);
        mainPanel.add(startServerButton);
    }

    /**
     * Layout components (GridBag being a personal favorite)
     */
    private void layoutComponents() {
        GridBagLayout gbLayout = new GridBagLayout();
        GridBagConstraints gbC = new GridBagConstraints();
        gbC.fill = GridBagConstraints.BOTH;

        //Default settings
        gbC.gridwidth = 1;
        gbC.gridheight = 1;
        gbC.weightx = 1;


        //default weight
        gbC.weighty = 1;

        gbC.gridy = 0;

        gbC.gridx = 0;
        gbC.gridwidth = 2;
        gbLayout.setConstraints(serverList, gbC);

        //reduced weight
        gbC.weighty = 0.05;

        gbC.gridy = 1;
        gbC.gridwidth = 1;

        gbC.gridx = 0;
        gbLayout.setConstraints(updateButton, gbC);

        gbC.gridx = 1;
        gbLayout.setConstraints(connectButton, gbC);

        gbC.gridy = 2;

        gbC.gridx = 0;
        gbC.gridwidth = 2;
        gbLayout.setConstraints(startServerButton, gbC);

        mainPanel.setLayout(gbLayout);
    }

    /**
     * sets the callbacks to components
     * All anonymous listeners.
     */
    private void setBehaviours() {

        serverList.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                //on doubleclick
                if (e.getClickCount() >= 2) {
                    //Click connect button
                    connectButton.doClick();
                }
            }

// <editor-fold defaultstate="collapsed" desc="Unused overrides">
            @Override
            public void mousePressed(MouseEvent e) {
            } //do nothing

            @Override
            public void mouseReleased(MouseEvent e) {
            }//do nothing

            @Override
            public void mouseEntered(MouseEvent e) {
            }//do nothing

            @Override
            public void mouseExited(MouseEvent e) {
            }//do nothing// </editor-fold>
        });

        //Update Button
        updateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                serverList.setListData(clientManager.getServerList(SERVER_SEARCH_TIME));
            }
        });

        //Connect Button
        connectButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (clientManager.isRunning()) {
                    clientManager.forceShow();
                    return;
                }

                //Do Connect
                if (serverList.getSelectedName() != null) {
                    final String msg = serverList.getSelectedName();

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            clientManager.connectTo(msg, SERVER_CONNECT_TIMEOUT);
                        }
                    }).start();

                } else {
                    JOptionPane.showMessageDialog(null, "Nothing Selected");
                }
            }
        });


        //Start Server Button
        startServerButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (server.isRunning()) {
                    server.stopServer();
                    startServerButton.setText("Start Server");

                } else {
                    String name = JOptionPane.showInputDialog("Enter Server Name");
                    if (name != null) {
                        server.setName(name);
                        server.startServer();
                        startServerButton.setText("Stop Server");
                    }
                }

                // Give small wait for server to start before updating serverlist
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger("Launcher").log(Level.SEVERE, "Thread got interupted");
                }
                updateButton.doClick();

            }
        });


    }
}
