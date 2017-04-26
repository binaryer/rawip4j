package lcy.rawip4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.primitives.Bytes;

public abstract class RxdUtil {
	
	public static long readpackage_ok = 0L; 
	public static long readpackage_chksumerr = 0L; 
	public static long readpackage_unexpected = 0L; 
	
	
	public static void readloop(final InputStream ins, final LinkedBlockingQueue<byte[]> queue) throws IOException, InterruptedException{
		
		final byte[] bs = new byte[1024*100];
		
		int receivedlength = 0;
		while(true){
			
			try{
				receivedlength += ins.read(bs, receivedlength, 1024*50);
			}catch(IndexOutOfBoundsException e){
				e.printStackTrace();
				System.err.println("receivedlength = " + receivedlength);
				System.exit(-1);
				return;
			}
			
			//System.out.println("receivedlength = " + receivedlength);
			
			if(receivedlength < PacketFrame.MAGIC.length)	continue;
			int indexfind = -1;
			if((indexfind=Bytes.indexOf(bs, PacketFrame.MAGIC)) >= 0){
				
				
				final PacketFrame packageframe = new PacketFrame();
				final int r = PacketFrame.read(bs, indexfind, receivedlength, packageframe);
				
				if(r == PacketFrame.read_tooshort){
					// 继续读
				}else if(r == PacketFrame.read_chksumerr){
					// 抛弃并记录日志
					//System.err.println("read_chksumerr " + new Date());
					readpackage_chksumerr++;
					
					//抛弃包头相当于抛弃了包
					System.arraycopy(bs, indexfind+PacketFrame.MAGIC.length, bs, 0, receivedlength-PacketFrame.MAGIC.length-indexfind);
					receivedlength -= (PacketFrame.MAGIC.length+indexfind);
					
				}else if(r == PacketFrame.read_unexpected){
					// 抛弃并记录日志
					//System.err.println("read_unexpected " + new Date());
					readpackage_unexpected++;
					
					System.arraycopy(bs, indexfind+PacketFrame.MAGIC.length, bs, 0, receivedlength-PacketFrame.MAGIC.length-indexfind);
					receivedlength -= (PacketFrame.MAGIC.length+indexfind);
					
				}else{
					// 处理后抛弃
					readpackage_ok++;
					queue.put(packageframe.getData());
					
					System.arraycopy(bs, indexfind, bs, 0, r);
					receivedlength -= r;
					
				}
				
			}else{
				//删除无用数据, 后(PackageFrame.MAGIC.length-1)个字节移到最前面
				//System.out.println("magic notfound");
				System.arraycopy(bs, receivedlength-PacketFrame.MAGIC.length+1, bs, 0, PacketFrame.MAGIC.length-1);
				receivedlength = PacketFrame.MAGIC.length-1;
			}
				
		}
	}

}
