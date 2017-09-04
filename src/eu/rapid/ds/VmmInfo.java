package eu.rapid.ds;

public class VmmInfo {
	private long vmmid;
	private String ipv4;
	private int mactype;
	private int freecpu;
	private int cpunums;
	private long freemem;
	private int freegpu;
	private int gpunums;
	private String availtypes;

	/**
	 * @return
	 */
	public long getVmmid() {
		return vmmid;
	}

	/**
	 * @param vmmid
	 */
	public void setVmmid(long vmmid) {
		this.vmmid = vmmid;
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
	public int getMactype() {
		return mactype;
	}

	/**
	 * @param mactype
	 */
	public void setMactype(int mactype) {
		this.mactype = mactype;
	}

	/**
	 * @return
	 */
	public int getFreecpu() {
		return freecpu;
	}

	/**
	 * @param freecpu
	 */
	public void setFreecpu(int freecpu) {
		this.freecpu = freecpu;
	}

	/**
	 * @return
	 */
	public int getCpunums() {
		return cpunums;
	}

	/**
	 * @param cpunums
	 */
	public void setCpunums(int cpunums) {
		this.cpunums = cpunums;
	}

	/**
	 * @return
	 */
	public long getFreemem() {
		return freemem;
	}

	/**
	 * @param freemem
	 */
	public void setFreemem(long freemem) {
		this.freemem = freemem;
	}

	/**
	 * @return
	 */
	public int getFreegpu() {
		return freegpu;
	}

	/**
	 * @param freegpu
	 */
	public void setFreegpu(int freegpu) {
		this.freegpu = freegpu;
	}

	/**
	 * @return
	 */
	public int getGpunums() {
		return gpunums;
	}

	/**
	 * @param gpunums
	 */
	public void setGpunums(int gpunums) {
		this.gpunums = gpunums;
	}

	/**
	 * @return
	 */
	public String getAvailtypes() {
		return availtypes;
	}

	/**
	 * @param availtypes
	 */
	public void setAvailtypes(String availtypes) {
		this.availtypes = availtypes;
	}
}
