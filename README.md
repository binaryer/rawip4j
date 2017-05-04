# rawip4j
java链路层封包协议, 实现数据包完整性校验 可用于无线收发模块(激光/可见光/2.4G/433Mhz/315Mhz ...)实现TCP/IP通信
java Data Link Layer protocol

## 说明
+ 没有重传确认功能, 发送不保证对方一定收到包，亦不保证顺序。需要结合 tun/tap 才能实现TCP通信
+ 如果收到包，则可保证包数据完整性(使用md5算法校验和)
+ 配合 tun/tap 使用，可实现多终端全双工通信, 建议MTU设置为256以下，恶劣环境下需设置更低的值
+ 虽然433Mhz, 315Mhz, Infrared-ray功耗低，但传输速率也较低，因此不适合用来浏览互联网，建议用在物联网少量数据传输场景

user-program -> tun/tap -> rawip4j -> wireless(zigbee, 433Mhz, 315Mhz, Infrared-ray) ->    (THE AIR) ->    wireless -> rawip4j -> tun/tap -> user-program

## 使用方法:
``` java
public static void main(String[] args) throws IOException, InterruptedException {

		// 定义队列用于存储接收到的包 received packet queue
		final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

		// 通过 rxtx 获取设备的InputStream 和 OutputStream
		//TODO get the InputStream & OutputStream from SerialPort devices
		// you can use librxtx-java (aptitude install librxtx-java)
		// or http://mvnrepository.com/artifact/org.rxtx/rxtx (untested)
		InputStream ins = null;
		OutputStream outs = null;




		/* *********************************************************************************************************************** */

		// 开始读取包，读到的包将放入BlockingQueue队列中
		// 这个方法是立即返回的(无阻塞)
		// 非线程安全, 如用于多线程场景，请做好加锁同步
		// receive packet into the queue
		RxdUtil.register(ins, queue);


		/* *********************************************************************************************************************** */

    // 新开线程处理接收到的包
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

		// 发送包，不保证对方一定接收到包，但如果接收到，则能保证包的数据完整性
   	 	// chksumlength: 校验和字节，可以设置为2-16，越大越安全, 建议8
		// send a data packet
		// chksumlength: use md5 to checksum a packet, the value can be 2-16, recommend 8
		new PacketFrame((byte)8, "hello, rawip4j".getBytes()).write(outs);


		/* *********************************************************************************************************************** */


		TimeUnit.SECONDS.sleep(Long.MAX_VALUE);


	}
```

## 效果图

433MHz模块距离20米，隔2墙1窗1门，ping丢包率5%, wget 600bytes/s  

![](https://github.com/binaryer/rawip4j/raw/master/2017-04-26-112556_759x827_scrot.png)  
![](https://github.com/binaryer/rawip4j/raw/master/IMG_20170426_122613.jpg)

单工激光通信

