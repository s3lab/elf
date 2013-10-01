package org.apache.hadoop.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class PrefetchAction extends TaskTrackerAction{

	private String filePath;    // file path to open
	private long offset;        // starting point
	private long len;           // bytes to read
    private String taskId;      // id of the prefetch task, same for JT and TT

	public PrefetchAction() {
		super(ActionType.PREFETCH);
	}
	
	public PrefetchAction(String filePath, long offset, long len, String tid){
		super(ActionType.PREFETCH);
		this.filePath = filePath;
		this.offset = offset;
		this.len = len;
        this.taskId = tid;
	}
	
	public void write(DataOutput out) throws IOException {
	    out.writeUTF(this.filePath);
	    out.writeLong(offset);
	    out.writeLong(len);
        out.writeUTF(this.taskId);
	}
	
	public void readFields(DataInput in) throws IOException {
		this.filePath = in.readUTF();
		this.offset = in.readLong();
		this.len = in.readLong();
        this.taskId = in.readUTF();
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public long getLen() {
		return len;
	}

	public void setLen(long len) {
		this.len = len;
	}

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
