package src.examples.org.apache.hadoop.examples;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;

public class RPCServer {
	
	public void startServer() throws IOException{
		Configuration conf = new Configuration();
		Server server = RPC.getServer(this, "localhost", 16000, conf);  // start a server on localhost:16000
		server.start();
	}
	public static void main(String[] args) throws IOException {
		RPCServer s = new RPCServer();
		s.startServer();
	}
}
