package eu.rapid.ds;

import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidUtils;
import eu.rapid.ds.VmType;

public class DirectoryServer {
	private static DirectoryServer directoryServer = new DirectoryServer();
	private Logger logger = Logger.getLogger(getClass());

	private int DSPort = 9001;
	private int maxConnection = 100;
	private int VMMPort = 9000;

	private String animationAddress = "83.235.169.221";
	private int animationServerPort = 6666;

	private VmType[] vmTypes;
	static final int maxAvailableVmType = 10;
	
	private SlamInfo slamInfo = null;
	
	private DirectoryServer() {
		try {
			readConfiguration();
			vmTypes = new VmType[DirectoryServer.maxAvailableVmType];
		} catch (Exception e) {
			logger.error("DirectoryServer Initialization is failed");

			String message = "";
			for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
				message = message + System.lineSeparator() + stackTraceElement.toString();
			}
			logger.error("Caught Exception: " + e.getMessage() + System.lineSeparator() + message);

			e.printStackTrace();
		}
	}

	public static DirectoryServer getInstance() {
		return directoryServer;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		DirectoryServer directoryServer = DirectoryServer.getInstance();

		directoryServer.getLogger().info("Directory Server Started");

		directoryServer.getLogger().info("AnimationAddress in DS is: " + directoryServer.getAnimationAddress());
		// DS is up
		//RapidUtils.sendAnimationMsg(directoryServer.getAnimationAddress(), directoryServer.getAnimationServerPort(),
		//		RapidMessages.AnimationMsg.DS_UP.toString());

		// initialize vmTypes. now hard coding, but later will use XML style
		// assignment.
		directoryServer.initializeVmTypes();

		try {
			ThreadPooledServer server = new ThreadPooledServer(directoryServer.getDSPort());
			new Thread(server).start();
		} catch (Exception e) {
			String message = "";
			for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
				message = message + System.lineSeparator() + stackTraceElement.toString();
			}
			directoryServer.getLogger().error("Caught Exception: " + e.getMessage() + System.lineSeparator() + message);

			e.printStackTrace();
		}
	}

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger
	 *            the logger to set
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	private void readConfiguration() {
		try {
			InputSource is = new InputSource(new FileReader("/home/rapid/rapid_vmm_ds/bin/configuration.xml"));
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			XPath xpath = XPathFactory.newInstance().newXPath();

			String expression = "//*/DS";
			NodeList cols = (NodeList) xpath.compile(expression).evaluate(document, XPathConstants.NODESET);

			for (int idx = 0; idx < cols.getLength(); idx++) {
				expression = "//*/vmmPort";
				String vmmPort = xpath.compile(expression).evaluate(document);
				setVMMPort(Integer.parseInt(vmmPort));

				expression = "//*/dsPort";
				String dsPort = xpath.compile(expression).evaluate(document);
				setDSPort(Integer.parseInt(dsPort));

				expression = "//*/dsMaxConnection";
				String dsMaxConnection = xpath.compile(expression).evaluate(document);
				setMaxConnection(Integer.parseInt(dsMaxConnection));

			}

		} catch (Exception e) {
			String message = "";
			for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
				message = message + System.lineSeparator() + stackTraceElement.toString();
			}
			logger.error("Caught Exception: " + e.getMessage() + System.lineSeparator() + message);
			e.printStackTrace();
		}
	}

	/**
	 * This function initializes the VM type. Currently, VM type 0 is only
	 * supported.
	 */
	public void initializeVmTypes() {
		for (int i = 0; i < DirectoryServer.maxAvailableVmType; i++)
			vmTypes[i] = new VmType();

		vmTypes[0].setId(0);
		vmTypes[0].setNumCore(1);
		vmTypes[0].setMemory(1024000); // memory in KB
		vmTypes[0].setDisk(5000); // disk in MB
		vmTypes[0].setGpuCore(512);
	}

	/**
	 * @return
	 */
	public int getDSPort() {
		return DSPort;
	}

	/**
	 * @param dSPort
	 */
	public void setDSPort(int dSPort) {
		DSPort = dSPort;
	}

	/**
	 * @return
	 */
	public int getMaxConnection() {
		return maxConnection;
	}

	/**
	 * @param maxConnection
	 */
	public void setMaxConnection(int maxConnection) {
		this.maxConnection = maxConnection;
	}

	/**
	 * @return
	 */
	public int getVMMPort() {
		return VMMPort;
	}

	/**
	 * @param vMMPort
	 */
	public void setVMMPort(int vMMPort) {
		VMMPort = vMMPort;
	}

	/**
	 * @return the animationAddress
	 */
	public String getAnimationAddress() {
		return animationAddress;
	}

	/**
	 * @param animationAddress
	 *            the animationAddress to set
	 */
	public void setAnimationAddress(String animationAddress) {
		this.animationAddress = animationAddress;
	}

	/**
	 * @return the animationServerPort
	 */
	public int getAnimationServerPort() {
		return animationServerPort;
	}

	/**
	 * @param animationServerPort
	 *            the animationServerPort to set
	 */
	public void setAnimationServerPort(int animationServerPort) {
		this.animationServerPort = animationServerPort;
	}

	/**
	 * @return
	 */
	public VmType[] getVmTypes() {
		return vmTypes;
	}

	/**
	 * @param vmTypes
	 */
	public void setVmTypes(VmType[] vmTypes) {
		this.vmTypes = vmTypes;
	}

	public SlamInfo getSlamInfo() {
		return slamInfo;
	}

	public void setSlamInfo(SlamInfo slamInfo) {
		this.slamInfo = slamInfo;
	}
}
