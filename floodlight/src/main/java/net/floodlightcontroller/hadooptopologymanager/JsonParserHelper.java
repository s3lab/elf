package net.floodlightcontroller.hadooptopologymanager;

//import com.fasterxml.jackson.core.JsonFactory;
//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonToken;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class JsonParserHelper {
    public static List<String> parseStringArray(String jsonStrArray) throws IOException {
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        List<String> l =
                mapper.readValue(jsonStrArray, new TypeReference<List<String>>() { });
        return l;
    }
    public static final String BAD_REQUEST = "{\"status\" : \"" + "400" + "\"}";
    public static final String OK = "{\"status\" : \"" + "202" + "\"}";

    static class GatewayRecord {
        public String getDcid() {
            return dcid;
        }

        public void setDcid(String dcid) {
            this.dcid = dcid;
        }

        public List<String> getGateways() {
            return gateways;
        }

        public void setGateway(String gateway) {
            this.gateways.add(gateway);
        }

        public GatewayRecord() {
            dcid = "";
            gateways = new LinkedList<String>();
        }

        private String dcid;
        private List<String> gateways;
    }

    public static List<GatewayRecord> parseDatacenterGateways(String js) throws IOException {
        List<GatewayRecord> result = new LinkedList<GatewayRecord>();
        JsonFactory jf = new JsonFactory();
        JsonParser jp =
                jf.createJsonParser(js);
        jp.nextToken(); // to first elem

        // loop each key
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            GatewayRecord gr = new GatewayRecord();
            String dcid = jp.getCurrentName();
            gr.setDcid(dcid);
            jp.nextToken();
            while(jp.nextToken() != JsonToken.END_ARRAY){
                gr.setGateway(jp.getText());
            }
            result.add(gr);
        }
        return result;
    }
}
