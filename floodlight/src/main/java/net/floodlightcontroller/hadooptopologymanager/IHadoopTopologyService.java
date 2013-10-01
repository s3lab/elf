package net.floodlightcontroller.hadooptopologymanager;

import net.floodlightcontroller.core.module.IFloodlightService;

import java.util.List;

public interface IHadoopTopologyService extends IFloodlightService {
    /**
     * For setting the core switch in a tree like topology
     * */
    public boolean setRootSwitch(String coreDpid);

    /**
     * For setting gateway switches in the topology for the given data center
     * */
    public boolean setGatewaySwitches(String dcid, List<String> gateways);

    /**
     * Get slash-separated string path like used in Hadoop
     * */
    public String getHostLocation(String hostIp);
}
