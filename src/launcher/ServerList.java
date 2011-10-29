package launcher;

import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

/**
 * Custom JList type.
 * Capable of storing and displaying server ip and name pairs
 * @author kjb146 and zjt14
 */
public class ServerList extends JList {

    /**
     * Inner class, represents the binding between a server name and IP
     */
    private class ServerID {

        private String name;
        private String ip;

        /**
         * Constructor, the only way to assign the values
         * (immutable)
         * @param name servers name
         * @param ip servers ip
         */
        public ServerID(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        /**
         * gets the servers name from the object
         * @return servers name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the servers ip from the object
         * @return servers ip
         */
        public String getIP() {
            return ip;
        }

        /**
         * Overrides the toString method to provide a "pretty" value for display
         * @return String representation of the object
         */
        @Override
        public String toString() {
            return name + "      <" + ip + ">";
        }
    }

    /**
     * Constructor
     * Does a tiny bit of configuration
     */
    public ServerList() {
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Get the name from the selected index
     * @return server name
     */
    public String getSelectedName() {
        ServerID t = (ServerID) this.getSelectedValue();
        return (t == null) ? null : t.getName();
    }

    /**
     * Get the ip from the selected index
     * @return server ip
     */
    public String getSelectedIP() {
        ServerID t = (ServerID) this.getSelectedValue();
        return (t == null) ? null : t.getIP();
    }

    /**
     * populates the JList from the given map data
     * @param tMap a <IP, String> mapping of server data
     */
    public void setListData(Map<String, String> tMap) {
        this.removeAll();

        DefaultListModel model = new DefaultListModel();
        for (String ip : tMap.keySet()) {
            ServerID t = new ServerID(ip, tMap.get(ip).trim());
            model.addElement(t);
        }

        this.setModel(model);
    }
}
