package net.floodlightcontroller.hadooptopologymanager;

//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class HadoopTopologyRootResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(HadoopTopologyRootResource.class);
    @Post
    public String store(String rootJson) {
        IHadoopTopologyService hds = (IHadoopTopologyService)
                getContext().getAttributes().get(IHadoopTopologyService.class.getCanonicalName());

        List<String> l = null;
        try {
            l = JsonParserHelper.parseStringArray(rootJson);
        } catch (IOException e) {
            log.error("Exception while parsing json root request");
            return JsonParserHelper.BAD_REQUEST;
        }

        // cannot parse
        if(l.isEmpty()){
            return JsonParserHelper.BAD_REQUEST;
        }

        // root parsed successfully
        boolean f =  hds.setRootSwitch(l.get(0));
        if(!f){
            // cant add to resource
            log.error("Cannot set root");
            return JsonParserHelper.BAD_REQUEST;
        } else {
            return JsonParserHelper.OK;
        }
    }
}
