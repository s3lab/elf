package net.floodlightcontroller.hadooptopologymanager;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import net.floodlightcontroller.hadooptopologymanager.JsonParserHelper.GatewayRecord;

public class HadoopTopologyGatewayResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(HadoopTopologyGatewayResource.class);

    @Post
    public String store(String gatewayJson) {
        IHadoopTopologyService hds = (IHadoopTopologyService)
                getContext().getAttributes().get(IHadoopTopologyService.class.getCanonicalName());

        List<GatewayRecord> l = null;
        try {
            l = JsonParserHelper.parseDatacenterGateways(gatewayJson);
        } catch (IOException e) {
            log.error("Exception while parsing json gateway request");
            return JsonParserHelper.BAD_REQUEST;
        }

        // empty gateway setting
        if(l.isEmpty()){
            return JsonParserHelper.BAD_REQUEST;
        }

        for(GatewayRecord gr: l){
            // root parsed successfully
            log.info("*********** setting gateway of " + gr.getDcid() + ":" + gr.getGateways().toString());
            boolean f =  hds.setGatewaySwitches(gr.getDcid(), gr.getGateways());
            if(!f){
                // can't add to resource
                log.error("Cannot set gateway for" + gr.getDcid());
                return JsonParserHelper.BAD_REQUEST;
            }
        }

        return JsonParserHelper.OK;
    }
}
