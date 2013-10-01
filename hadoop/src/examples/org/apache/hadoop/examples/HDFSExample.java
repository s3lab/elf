package org.apache.hadoop.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.util.CacheBlock;

public class HDFSExample {
	public static final String theFilename = "input/in";
	public static final String message = "Hello, world!\n";
	private static int BLK_SIZE = 1024*1024*64;

	public static void check(CacheBlock blk) throws IOException {
		byte[] outputBuf = new byte[BLK_SIZE];
		blk.getByte(0, BLK_SIZE, outputBuf, 0);

		FileOutputStream fos = new FileOutputStream("/tmp/output");
		fos.write(outputBuf);
		fos.close();
	}
	
	public static void main(String[] args) throws IOException {

		Configuration conf = new Configuration();
		conf.addResource(new Path("/home/codefish/workspace/flexfetch/src/conf/hdfs-site.xml"));
		conf.addResource(new Path("/home/codefish/workspace/flexfetch/src/conf/core-site.xml"));
		FileSystem fs = FileSystem.get(conf);
		System.out.println("file system uri=" + fs.getUri());
		System.out.println(fs instanceof DistributedFileSystem);
			
		Path filenamePath = new Path(theFilename);
		try {
			FSDataInputStream in = fs.open(filenamePath);
			CacheBlock blk = new CacheBlock(1, BLK_SIZE);
			blk.bindInput(in);
			blk.writeWholeBlock(0, BLK_SIZE);
			
			check(blk);
			/*
			if (fs.exists(filenamePath)) {
				System.out.println("file " + filenamePath.toString() + " existed");
			}
			
			FSDataOutputStream out = fs.create(filenamePath);
			out.writeUTF(message);
			out.flush();
			out.close();

			FSDataInputStream in = fs.open(filenamePath);
			String messageIn = in.readUTF();
			System.out.print(messageIn);
			in.close();
			*/
		} catch (IOException ioe) {
			System.err.println("IOException during operation: "
					+ ioe.toString());
			System.exit(1);
		}
	}
}
