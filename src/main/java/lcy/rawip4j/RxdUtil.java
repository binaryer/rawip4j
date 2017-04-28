package lcy.rawip4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import com.google.common.primitives.Bytes;

public abstract class RxdUtil {
	
	public static long readed_total_bytes = 0L;	//读取到的总字节
	
	public static long readpackage_ok = 0L; 			//正常包数量
	public static long readpackage_ok_bytes = 0L; 		//正常包字节数
	
	public static long readpackage_chksumerr = 0L; 	//校验失败包数量
	public static long readpackage_chksumerr_bytes = 0L; 	//校验失败包字节数
	
	public static long readpackage_unexpected = 0L; 	//畸形包数量, 畸形包没有'字节'的概念
	
	private static Set<InputStream> registeredins = new HashSet<>();
	public static void register(final InputStream ins, final BlockingQueue<byte[]> queue) {
		
		if(registeredins.contains(ins)){
			throw new RuntimeException("the InputStream has been registered");
		}
		registeredins.add(ins);
		
		
		final Thread t = new Thread(new Runnable() {
			
			final byte[] bs = new byte[1024*100];
			int receivedlength = 0;
			@Override
			public void run() {
				while(true){
					
					try{
						 final int r = ins.read(bs, receivedlength, 1024*50);
						 receivedlength += r;
						 readed_total_bytes += r;
					}catch(IndexOutOfBoundsException e){
						e.printStackTrace();
						System.err.println("receivedlength = " + receivedlength);
						return;
					}catch(IOException e){
						e.printStackTrace();
						return;
					}
					
					//System.out.println("receivedlength = " + receivedlength);
					
					if(receivedlength < PacketFrame.MAGIC.length)	continue;
					int indexfind = -1;
					if((indexfind=Bytes.indexOf(bs, PacketFrame.MAGIC)) >= 0){
						
						final PacketFrame packageframe = new PacketFrame();
						final int[] r = PacketFrame.read(bs, indexfind, receivedlength, packageframe);
						
						if(r[0] == PacketFrame.read_tooshort){
							// 继续读
							
						}else if(r[0] == PacketFrame.read_chksumerr){
							readpackage_chksumerr++;
							
							readpackage_chksumerr_bytes += r[1];
							
							//抛弃包头相当于抛弃了包
							System.arraycopy(bs, indexfind+PacketFrame.MAGIC.length, bs, 0, receivedlength-PacketFrame.MAGIC.length-indexfind);
							receivedlength -= (PacketFrame.MAGIC.length+indexfind);
							
						}else if(r[0] == PacketFrame.read_unexpected){
							readpackage_unexpected++;
							
							System.arraycopy(bs, indexfind+PacketFrame.MAGIC.length, bs, 0, receivedlength-PacketFrame.MAGIC.length-indexfind);
							receivedlength -= (PacketFrame.MAGIC.length+indexfind);
							
						}else if(r[0] == PacketFrame.read_ok){
							readpackage_ok++;
							readpackage_ok_bytes += r[1];
							try {
								queue.put(packageframe.getData());
							} catch (InterruptedException e) {
								e.printStackTrace();
								return;
							}
							
							System.arraycopy(bs, indexfind, bs, 0, r[1]);
							receivedlength -= r[1];
							
						}else{
							throw new RuntimeException("unknow return value "+r[0]);
						}
						
					}else{
						//删除无用数据, 后(PackageFrame.MAGIC.length-1)个字节移到最前面
						//System.out.println("magic notfound");
						System.arraycopy(bs, receivedlength-PacketFrame.MAGIC.length+1, bs, 0, PacketFrame.MAGIC.length-1);
						receivedlength = PacketFrame.MAGIC.length-1;
					}
						
				}
				
			}
		});
		t.setName("rawip4j register-read thread " + ins.hashCode());
		t.setDaemon(true);
		t.start();
		
	}

}
