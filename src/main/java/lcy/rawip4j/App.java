package lcy.rawip4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class App {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		// received packet queue
		final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
				
				
		//TODO get the InputStream & OutputStream from SerialPort devices
		// you can use librxtx-java (aptitude install librxtx-java)
		// or http://mvnrepository.com/artifact/org.rxtx/rxtx (untested)
		InputStream ins = null;
		OutputStream outs = null;
		
		
		

		/* *********************************************************************************************************************** */
		
		
		// start a thread to receive packet into the queue
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					RxdUtil.readloop(ins, queue);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		
		
		/* *********************************************************************************************************************** */
		
		// start a received packet handler thread
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true){
					try {
						final byte[] data = queue.take();
						System.out.println("received packet: " + new String(data));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		
		/* *********************************************************************************************************************** */
		
		
		// send a data packet
		// chksumlength: use md5 to checksum a packet, the value can be 2-16, recommend 8
		new PacketFrame((byte)8, "hello, world".getBytes()).write(outs);
		
		
		/* *********************************************************************************************************************** */
		
		
		TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
		
		
	}
}
