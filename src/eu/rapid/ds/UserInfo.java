package eu.rapid.ds;

public class UserInfo {
	private long userid;
	private String ipv4;
	private String qosparam;

	/**
	 * @return
	 */
	public long getUserid() {
		return userid;
	}

	/**
	 * @param userid
	 */
	public void setUserid(long userid) {
		this.userid = userid;
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
	public String getQosparam() {
		return qosparam;
	}

	/**
	 * @param qosparam
	 */
	public void setQosparam(String qosparam) {
		this.qosparam = qosparam;
	}
}
