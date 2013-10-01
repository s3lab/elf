package org.apache.hadoop.mapred;

import org.apache.hadoop.mapreduce.split.JobSplit.TaskSplitMetaInfo;

public class PrefetchTask {
	
	private TaskSplitMetaInfo split = null;					// call tip's method to as getter
	private volatile boolean isActive;
	private PrefetchInfo prefetchInfo;
    private String taskId;

    private TaskInProgress tip;

    public PrefetchTask(TaskSplitMetaInfo split, TaskInProgress tip){
		this.split = split;
		this.isActive = false;
		this.prefetchInfo = new PrefetchInfo(split.getSplitIndex().getSplitLocation(),
												   split.getStartOffset(),
												   split.getInputDataLength());
        this.taskId = tip.getTIPId() + "_prefetch";
        this.tip = tip;
	}
	public boolean isActive() {
		return isActive;
	}
	public void setActive() {
		this.isActive = true;
	}
	public void setInactive() {
		this.isActive = false;
	}
	
	public static class PrefetchInfo {
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
		private String filePath;
		private long offset;
		private long len;
		public PrefetchInfo(String filePath, long offset, long len) {
			super();
			this.filePath = filePath;
			this.offset = offset;
			this.len = len;
		}
	}
	
	public String getFilePath(){
		return prefetchInfo.getFilePath();
	}
	public long getOffset(){
		return prefetchInfo.getOffset();
	}
	public long getLen(){
		return prefetchInfo.getLen();
	}

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskInProgress getTip() {
        return tip;
    }
}
