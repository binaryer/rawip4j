package lcy.rawip4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

public class PacketFrame {

	public static final byte[] MAGIC = { 5, -3, 126, 120, -18, 24, 92, 1 };	//可自定义，通讯双方必须一致

	private final byte[] magic = new byte[MAGIC.length];
	private short datalength; // 数据长度
	private byte[] data; // 数据
	private byte chksumlength; // 校验和长度
	private byte[] chksum; // 校验和

	public byte[] getChksum() {
		return chksum;
	}

	public PacketFrame() {
	}

	public PacketFrame(byte chksumlength, byte[] data) {
		setChksumlength(chksumlength);
		setData(data);
	}

	public byte getChksumlength() {
		return chksumlength;
	}

	public void setChksumlength(byte chksumlength) {
		this.chksumlength = chksumlength;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void write(OutputStream out) throws IOException {

		if (data.length == 0 || data.length > (Short.MAX_VALUE - Short.MIN_VALUE + 1))
			throw new RuntimeException("data length must be 1-65536, current " + data.length);
		if (chksumlength < 2 || chksumlength > 16)
			throw new RuntimeException("chksumlength must be 2-16, current " + chksumlength);

		System.arraycopy(MAGIC, 0, magic, 0, MAGIC.length);
		out.write(magic);
		
		datalength = (short) data.length;
		out.write(ByteBuffer.allocate(2).putShort(datalength).array());
		out.write(data);
		out.write(new byte[]{chksumlength});
		out.write(chksum = ArrayUtils.subarray(DigestUtils.md5(data), 0, chksumlength));
		
		/*
		System.out.println("writePackage");
		for(byte b : magic){
			System.out.print(b+",");
		}
		for(byte b :  ByteBuffer.allocate(2).putShort(datalength).array()){
			System.out.print(b+",");
		}
		*/
		
	}
	
	// 返回整个PackageFrame长度字节, 包含magic
	// 长度未满 -1
	// 校验和失败 -2
	// 格式错误意料外数据 -3
	public static final int read_ok = 0;
	public static final int read_tooshort = -1;
	public static final int read_chksumerr = -2;
	public static final int read_unexpected = -3;

	/**
	 * 
	 * @param bs
	 * @param startindex
	 * @param bslength
	 * @param packageframe
	 * @return	[是否成功, 包长]
	 */
	public static int[] read(byte[] bs, int startindex, int bslength, final PacketFrame packageframe) {
		
		final ByteBuffer bb = ByteBuffer.wrap(bs, startindex, bslength);
		for(int i=0; i<MAGIC.length; i++){
			packageframe.magic[i] = bb.get();
		}

		if (bslength < bb.position()+2)
			return new int[]{read_tooshort, -1};
		packageframe.datalength = bb.getShort();
		if(packageframe.datalength<1)	return new int[]{read_unexpected, -1};
		//System.out.println("startindex="+startindex +" bslength="+bslength+ " bb.position()="+bb.position()+" packageframe.datalength = " + packageframe.datalength);

		if (bslength < bb.position()+packageframe.datalength)
			return new int[]{read_tooshort, -1};
		
		packageframe.data = new byte[packageframe.datalength];
		for(int i=0; i<packageframe.datalength; i++){
			packageframe.data[i] = bb.get();
		}

		if (bslength < bb.position()+1)
			return new int[]{read_tooshort, -1};
		packageframe.chksumlength = bb.get();
		
		if(packageframe.chksumlength<1)	return new int[]{read_unexpected, -1};

		if (bslength < bb.position()+packageframe.chksumlength)
			return new int[]{read_tooshort, -1};

		final byte[] chksumbs = new byte[packageframe.chksumlength];
		for(int i=0; i<packageframe.chksumlength; i++){
			chksumbs[i] = bb.get();
		}
		
		
		if(!Arrays.equals(chksumbs, ArrayUtils.subarray(DigestUtils.md5(packageframe.data), 0, packageframe.chksumlength))){ 
			return new int[]{read_chksumerr, bb.position()};
		}
		
		return new int[]{read_ok, bb.position()};

	}


}
