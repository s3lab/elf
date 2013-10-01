package org.apache.hadoop.hdfs.protocol;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ipc.VersionedProtocol;

public interface PrefetchProtocol extends VersionedProtocol {
	public static final long versionID = 1L;
	//PrefetchResponse isPrefetched(long bid);
	//PrefetchResponse doPrefetch(Path fileName, long offset, long len);
	public void addBlock(long bid, String path);
	public void deleteBlock(long bid);
	public String getPrefetchedBlockPath(long bid); // return null if not prefetched
	//public void hasThisBlockPrefetched();
}