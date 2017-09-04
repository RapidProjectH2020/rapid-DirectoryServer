package eu.rapid.ds;

public class VmType {
	private int id;
	private int numCore;
	private long memory;
	private int disk;
	private int gpuCore;

	/**
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return
	 */
	public int getNumCore() {
		return numCore;
	}

	/**
	 * @param numCore
	 */
	public void setNumCore(int numCore) {
		this.numCore = numCore;
	}

	/**
	 * @return
	 */
	public long getMemory() {
		return memory;
	}

	/**
	 * @param memory
	 */
	public void setMemory(long memory) {
		this.memory = memory;
	}

	/**
	 * @return
	 */
	public int getDisk() {
		return disk;
	}

	/**
	 * @param disk
	 */
	public void setDisk(int disk) {
		this.disk = disk;
	}

	/**
	 * @return
	 */
	public int getGpuCore() {
		return gpuCore;
	}

	/**
	 * @param gpuCore
	 */
	public void setGpuCore(int gpuCore) {
		this.gpuCore = gpuCore;
	}
}
