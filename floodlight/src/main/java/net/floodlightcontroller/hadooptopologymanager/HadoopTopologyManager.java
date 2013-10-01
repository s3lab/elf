package net.floodlightcontroller.hadooptopologymanager;

import java.lang.Long;
import java.lang.String;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import org.apache.commons.lang.StringUtils;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.restserver.IRestApiService;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import net.floodlightcontroller.devicemanager.IDeviceService;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.*;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.*;
import org.jgrapht.graph.*;

public class HadoopTopologyManager implements IFloodlightModule, ILinkDiscoveryListener,
        IOFMessageListener, IHadoopTopologyService {

    //
    // Metadata area
    //

    protected IFloodlightProviderService floodlightProvider;
    protected IRestApiService restApi;
    protected IDeviceService deviceManager;
    protected ILinkDiscoveryService linkManager;
    protected static Logger log = LoggerFactory.getLogger(HadoopTopologyManager.class);
    private UndirectedGraph<String, DefaultEdge> internalTopology;


    // These two are set by network administrator
    private String root;
    private Map<String, String> gateway2dcMapping;

    // These three are set automatically by elf
    private Map<String, String> host2torMapping;
    private Map<String, String> tor2dcMapping;
    private Map<String, String> cachedMapping;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IHadoopTopologyService.class);
        //l.add(IRestApiService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        m.put(IHadoopTopologyService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IDeviceService.class);
        l.add(ILinkDiscoveryService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.restApi = context.getServiceImpl(IRestApiService.class);
        this.deviceManager = context.getServiceImpl(IDeviceService.class);
        this.linkManager = context.getServiceImpl(ILinkDiscoveryService.class);

        this.host2torMapping = new HashMap<String, String>();
        this.tor2dcMapping = new HashMap<String, String>();
        this.gateway2dcMapping = new HashMap<String, String>();
        this.root = DEFAULT_ROOT;
        this.internalTopology = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
        this.cachedMapping = new HashMap<String, String>();
    }

    private static final String DEFAULT_ROOT = "/default-root";
    private static final String DEFAULT_DC = "/default-dc";
    private static final String DEFAULT_TOR = "/default-rot";

    @Override
    public void startUp(FloodlightModuleContext context) {
        restApi.addRestletRoutable(new HadoopTopologyWebRoutable());
        linkManager.addListener(this);
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        return Command.CONTINUE;
    }

    @Override
    public String getName() {
        return "HadoopTopologyManager";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    //
    // REST API service
    //
    /**
     * This function is called when REST API is called to mark which switch
     * in the network is the root
     * @param rootDpid is the dpid of the root switch
     * */
    @Override
    public boolean setRootSwitch(String rootDpid) {

        // TODO: sannity check
        log.info("Adding root switch " + rootDpid + " of the topology");

        // Retrieve the internal network topology
        initInternalTopology();
        this.root = "/" + rootDpid;
        return true;
    }

    private void initInternalTopology(){
        // 1. init internal graph
        Map<Link, LinkInfo> ld = this.linkManager.getLinks();
        for(Link l: ld.keySet()){
            String src = HexString.toHexString(l.getSrc());
            String dst = HexString.toHexString(l.getDst());
            internalTopology.addVertex(src);
            internalTopology.addVertex(dst);
            internalTopology.addEdge(src, dst);
        }

        // 2. map TOR switch to data center
        List<String> tors = getLeaves(internalTopology);
        for(String tor: tors){
            this.tor2dcMapping.put(tor, this.root);
        }
    }

    /*
    * Finding the gateway of a given tor switch.
    * The basic idea is to use BFS on the topology graph.
    * The first "gateway" node that BFS encounters is the gateway
    * of the data center that it belongs to
    * */
    private String bfsToGateway(String tor) {
        BreadthFirstIterator<String, DefaultEdge> bfsItr = new
                BreadthFirstIterator<String, DefaultEdge>(internalTopology, tor);
        while(bfsItr.hasNext()){
            String n = bfsItr.next();
            if(gateway2dcMapping.containsKey(n)){
                return n;
            }
        }
        return null;
    }

    private List<String> getLeaves(UndirectedGraph<String, DefaultEdge> internalTopology) {
        Collection<String> vertexes = internalTopology.vertexSet();
        List<String> leaves = new LinkedList<String>();
        for(String v: vertexes){
            if(internalTopology.degreeOf(v) == 1){
                leaves.add(v);
            }
        }
        return leaves;
    }

    /**
     * This function is called by REST API to mark out the gateway swtiches of a
     * data center
     * @param datacenterId is the name of the data center
     * @param gateways are the gateways of this data center
     * */
    @Override
    public boolean setGatewaySwitches(String datacenterId, List<String> gateways) {
        // TODO: sannity check
        for(String gw: gateways){
            this.gateway2dcMapping.put(gw, datacenterId);
        }

        // 2. use BFS to compute mapping between tor and dc
        List<String> tors = getLeaves(internalTopology);
        for(String tor: tors){
            String gw = bfsToGateway(tor);
            if(gw != null){
                // not associated with any dc
                this.tor2dcMapping.put(tor, this.gateway2dcMapping.get(gw));
            }
        }
        return true;
    }

    private String getFromCache(String hostId){
        return this.cachedMapping.get(hostId);
    }

    /** This function is called by the REST API to query the network location
     * of a known host. It's the core function serving the Hadoop to achieve
     * the network locality
     * @param hostId the host name
     * @return a string that represent the network location of the host, which
     *  is separated by slashes, eg. /root-switch/datacenter-gateway/tor-switch
     *  NOTE:
     *   - we assume that the host is previously known to the system, meaning
     *   that it has sent traffic before. This is the basic assumption of Floodlight
     *   itself for a host to be detected.
     *   - we do *not* add the host name to the end of the return value.
     * */
    @Override
    public String getHostLocation(String hostId) {
        //TODO: sannity check

        // 0. Try getting from cache, if exist, just return it
        String hostPath = getFromCache(hostId);
        if(hostPath != null){
            return hostPath;
        }

        // We do not have it in cache, we need to query the floodlight controller to figure it out

        // 1. Get tor using host ip
        String tor = null;
        Long torId = null;

        // Query TOR given host ip from device manager
        Iterator<? extends IDevice> di =
                deviceManager.queryDevices(null, null, IPv4.toIPv4Address(hostId), null, null);
        while (di.hasNext()){
            Device d = (Device) di.next();
            SwitchPort[] switchPorts = d.getAttachmentPoints();
            torId = switchPorts[0].getSwitchDPID();
            tor = HexString.toHexString(switchPorts[0].getSwitchDPID());
        }

        if (tor == null || torId == null) {
            //can't get tor
            return DEFAULT_ROOT + DEFAULT_DC + DEFAULT_TOR + hostId;
        }
        this.host2torMapping.put(hostId, tor);

        // 2. Get dc using TOR
        String dc = this.tor2dcMapping.get(tor);
        if (dc == null) {
            //cant get dc
            return DEFAULT_ROOT + DEFAULT_DC + DEFAULT_TOR + hostId;
        }

        // 3. Form the result and return it
        String result = this.root + "/" + dc + "/" + tor;
        //put in cache
        cachedMapping.put(hostId, result);
        return result;
    }

    @Override
    public void linkDiscoveryUpdate(LDUpdate update) {
        // do nothing, dummy func
    }

    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
        // do nothing, dummy func
    }
}
