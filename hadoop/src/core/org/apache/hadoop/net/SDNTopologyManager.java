package org.apache.hadoop.net;

import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.*;
import org.mortbay.util.ajax.JSON;

public class SDNTopologyManager extends CachedDNSToSwitchMapping
        implements Configurable {

    // Well, I do not have resolve function, please go and check in cache
    //  by calling parent's resolve(), there, if it cannot resolve, it
    //  will call the RawFloodlightTopoManager's resolve to really resolve it

    // TODO: these should be put in ?? .xml file?
    public static final String FLOODLIGHT_CONTROLLER_IP = "topology.controller.ip";
    public static final String FLOODLIGHT_CONTROLLER_PORT = "topology.controller.port";
    private static Log LOG = LogFactory.getLog(ScriptBasedMapping.class);

    @Override
    public void setConf(Configuration conf) {
        ((RawFloodlightTopoManager)rawMapping).setConf(conf);
    }

    @Override
    public Configuration getConf() {
        return ((RawFloodlightTopoManager)rawMapping).getConf();
    }

    // call parent to register itself
    public SDNTopologyManager() {
        super(new RawFloodlightTopoManager());
    }

    public SDNTopologyManager(Configuration conf) {
        this();
        setConf(conf);
    }

    private static final class RawFloodlightTopoManager implements DNSToSwitchMapping {

        private Configuration conf;

        private RawFloodlightTopoManager() {
        }

        private RawFloodlightTopoManager(Configuration conf) {
            initialize(conf);
        }

        @Override
        public List<String> resolve(List<String> names) {
            /* ==> old tor-aware only stuff
            List <String> switchNames = new ArrayList<String>(names.size());
            for (int i = 0; i < names.size(); i++) {
                try {
                    switchNames.add(parseSwitchId(httpGet(restPrefix + names.get(i))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return switchNames;
            */
            List <String> hostLocation = new ArrayList<String>(names.size());
            for (String hn : names) {
                //LOG.info("==elf== resolving name " + hn);
                try {
                    hostLocation.add(parseNetworkLocation(httpGet(restPrefix + hn)));
                } catch (IOException e) {
                    LOG.error(e.toString());
                    // assume it belongs to the default path
                    hostLocation.add("/default-root/default-dc/default-tor/" + hn);
                }
            }
            return hostLocation;
        }

        private String parseNetworkLocation(String s) throws IOException {
            //LOG.info("==elf== parsing rest response " + s);
            if(s.startsWith("{")){
                throw new IOException("Invalid return from floodlight hadoop topology manager");
            } else {
                return s;
            }
        }

        @Deprecated
        private String parseSwitchId(String jsonResp) throws IndexOutOfBoundsException {
            Object entire = Array.get(JSON.parse(jsonResp), 0); // parse from string the obj to array, and get the first ele
            Map m = (Map)entire; // turn into a map
            Map ap = (Map)(Array.get(m.get("attachmentPoint"), 0)); // get ap from the map, which is an array whose first ele is a map
            return (String) ap.get("switchDPID");
        }

        public void setConf (Configuration conf) {
            this.conf = conf;
            this.controllerIp = conf.get(FLOODLIGHT_CONTROLLER_IP, "0.0.0.0");
            this.port = conf.getInt(FLOODLIGHT_CONTROLLER_PORT, 8080);
            //LOG.info("==elf== sdn controller url is " + controllerIp + ":" + port);
            this.restPrefix = "http://" + this.controllerIp + ":" +
                    Integer.toString(this.port) + "/wm/hadooptopology/?host=";
        }

        public Configuration getConf() {
            return conf;
        }

        private String controllerIp;
        private int port;
        private String restPrefix;


        private void initialize(Configuration conf){
            this.controllerIp = conf.get(FLOODLIGHT_CONTROLLER_IP);
            this.port = conf.getInt(FLOODLIGHT_CONTROLLER_PORT, 8080);
        }

        // Given a url of rest resource, return the response as a string
        // TODO: sanity check, e.g. is this url valid? is response empty?
        public String httpGet(String urlStr) throws IOException {
            URL url = new URL(urlStr);

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            if (conn.getResponseCode() != 200) {
                throw new IOException(conn.getResponseMessage());
            }

            // Buffer the result into a string
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

            conn.disconnect();
            return sb.toString();
        }
    }

    public String restGet(String url) throws IOException {
        return ((RawFloodlightTopoManager)rawMapping).httpGet(url);
    }
}
