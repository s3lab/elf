package net.floodlightcontroller.hadooptopologymanager;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

public class HadoopHostLocationResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(HadoopHostLocationResource.class);

    @Get("json")
    public String resolve() {
        IHadoopTopologyService hds = (IHadoopTopologyService)
                getContext().getAttributes().get(IHadoopTopologyService.class.getCanonicalName());
        Form form = getQuery();
        String ipv4Str = form.getFirstValue("host", true);
        // should not return null if correctly setting the host name
        String result = hds.getHostLocation(ipv4Str);
        if(result == null){
            log.debug("Cannot resolve host, set it to default");
        }
        return result;
    }
}
