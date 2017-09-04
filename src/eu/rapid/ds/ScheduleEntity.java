package eu.rapid.ds;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ScheduleEntity {
	private long vmid;
	private String ipv4;
	private int numRequest;
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	/**
	 * @return
	 */
	public long getVmid() {
		return vmid;
	}

	/**
	 * @param vmid
	 */
	public void setVmid(long vmid) {
		this.vmid = vmid;
	}

	/**
	 * @return
	 */
	public String getIpv4() {
		return ipv4;
	}

	/**
	 * @param ipv4
	 */
	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}

	/**
	 * @return
	 */
	public int getNumRequest() {
		return numRequest;
	}

	/**
	 * @param numRequest
	 */
	public void setNumRequest(int numRequest) {
		this.numRequest = numRequest;
	}

	/**
	 * @return
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @param socket
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	/**
	 * @return
	 */
	public ObjectInputStream getIn() {
		return in;
	}

	/**
	 * @param in
	 */
	public void setIn(ObjectInputStream in) {
		this.in = in;
	}

	/**
	 * @return
	 */
	public ObjectOutputStream getOut() {
		return out;
	}

	/**
	 * @param out
	 */
	public void setOut(ObjectOutputStream out) {
		this.out = out;
	}

}
