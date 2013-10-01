package src.mapred.org.apache.hadoop.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PrefetchResponse implements org.apache.hadoop.io.Writable {
	private String prefetchedFileURL;
	public PrefetchResponse(){
		
	}
	
	public PrefetchResponse(String blkURL) {
		prefetchedFileURL = blkURL;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.prefetchedFileURL = in.readUTF();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(this.prefetchedFileURL);
	}

	public String getBlockPath(){
		return this.prefetchedFileURL;
	}
}
