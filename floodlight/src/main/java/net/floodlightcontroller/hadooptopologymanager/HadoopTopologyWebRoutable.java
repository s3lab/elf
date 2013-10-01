package net.floodlightcontroller.hadooptopologymanager;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class HadoopTopologyWebRoutable implements RestletRoutable {

    /**
     * Create the Restlet router and bind to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/", HadoopHostLocationResource.class);
        router.attach("/root/json", HadoopTopologyRootResource.class);
        router.attach("/gateway/json", HadoopTopologyGatewayResource.class);
        return router;
    }

    /**
     * Set the base path for the Firewall
     */
    @Override
    public String basePath() {
        return "/wm/hadooptopology";
    }
}
