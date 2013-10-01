package org.apache.hadoop.hdfs.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.DataOutputStream;

import org.apache.hadoop.hdfs.DFSClient.DFSInputStream;

/* An HDFS block that is cached in memory 
 * Implemented in memory-mapped IO
 */
public class CacheBlock {
	private long blkId = 0;
	private long maxSize;//max blk size in byte
	private long size;// data size in byte
	//private MappedByteBuffer inMemBlkBuffer = null;
	private byte[] inMemBlkBuffer = null;
	private InputStream ins = null;
	
	private boolean isFree;
	public boolean isFree() {
		return isFree;
	}
	private boolean isCompleteBlock;
	public boolean isCompleteBlock() {
		return isCompleteBlock;
	}
	public void markAsComplete() {
		this.isCompleteBlock = true;
	}
	public void markAsIncomplete() {
		this.isCompleteBlock = false;
	}
	private final static int NUMBER_OF_BYTES_PER_READ = 1024*1024*64;
	
	//private DataOutputStream os = null;
	public CacheBlock(long blkId, int blkSize) {
		this.blkId = blkId;
		this.maxSize = blkSize;
		this.size = 0;
		this.inMemBlkBuffer = new byte[blkSize];
		this.isFree = false;
		this.isCompleteBlock = false;
				/*
				new RandomAccessFile(Long.toString(blkId), "rw").getChannel()
				.map(FileChannel.MapMode.READ_WRITE, 0, len);
				*/
	}
	
	public void writeByte(byte b) throws IOException{
		//inMemBlkBuffer.put(b);
		throw new IOException("This function should not be called because it's not supported");
	}
	
	public void writeByteBuff(byte[] bb) throws IOException {
		//inMemBlkBuffer.put(bb);
		throw new IOException("This function should not be called because it's not supported");
	}
	
	public void bindInput(InputStream inputStream){
		this.ins = inputStream;
	}
	
	/**
	 * PRE-REQUEST: the input flow is open
	 * This function write the whole block 
	 * starting from startPosition in the file, and read len bytes.
	 * The read data is stored in imMemBlkBuffer
	 * */
	synchronized public void writeWholeBlock(long startPosition, long len) {
		try{
			activate();
			long remaining = len;
			int offsetInBuf = 0;
			int offsetInStream = 0;
			ins.skip(startPosition);
			while(remaining > 0){
				long toRead = Math.min(NUMBER_OF_BYTES_PER_READ, remaining);
				
				int nread = ins.read(inMemBlkBuffer, 
										offsetInBuf, (int)toRead);
				if(nread != toRead){
					throw new IOException("Read len is not as expected");
				}
				offsetInStream += toRead;
				offsetInBuf += toRead;
				remaining -= toRead;
//DFSClient.LOG.info("==flexfetch== Cache block " + blkId + " read data of len " + toRead);
			}
			this.size = len;
//DFSClient.LOG.info("==flexfetch== Cache block read complete");
		} catch (IOException ioe){
			System.err.println("IOException during operation: "
					+ ioe.toString());
		}
	}
	
	/**
	 * Read data of length len of the block starting from startPosition,
	 * and put in to the targetBuf starting from offset 
	 * */
	synchronized public void getByte(int startPosition, int len, byte[] targetBuf, int offset) 
			throws IOException {
		if(len - startPosition > targetBuf.length - offset){
			throw new IOException("Buffer overloaded");
		}
		
		System.arraycopy(inMemBlkBuffer, startPosition, targetBuf, offset, len);
	}
	
	synchronized public void activate(){
		this.size = 0;
		this.isFree = true;
	}
	
	public static String CACHE_DIR_PREFIX = "/dev/shm/hadoop-cache-";
	public synchronized String flush(){
		FileOutputStream fos = null;
		String path = null;
		try{
			path = CACHE_DIR_PREFIX + Long.toString(this.blkId);
			fos = new FileOutputStream(path);
	        fos.write(this.inMemBlkBuffer);
	        fos.close();
		} catch (IOException ioe){
			System.err.println("IOException during flush: "
					+ ioe.toString());
		} 	       
        return path;
	}
}
