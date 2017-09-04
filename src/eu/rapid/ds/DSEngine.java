package eu.rapid.ds;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidUtils;
import eu.rapid.ds.VmInfo;

public class DSEngine {
	private static DSEngine dsEngine = new DSEngine();
	private Logger logger = Logger.getLogger(getClass());

	private Map<Long, ArrayList<VmInfo>> parallelMap;
	private List<ScheduleEntity> parallelQueue;

	private DSEngine() {
		parallelMap = Collections.synchronizedMap(new Hashtable<Long, ArrayList<VmInfo>>());
		parallelQueue = Collections.synchronizedList(new ArrayList<ScheduleEntity>());

		// start timer for scheduling forwarding and parallel execution jobs.
		ParallelTimerTask parallelTimerTask = new ParallelTimerTask();
		Timer vmmTimer = new Timer();
		vmmTimer.schedule(parallelTimerTask, 500, 500);
	}

	public static DSEngine getInstance() {
		return dsEngine;
	}

	/**
	 * The function deals with the SLAM_REGISTER_DS message. It registers the
	 * SLAM to the DS.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void slamRegisterDs(ObjectInputStream in, ObjectOutputStream out) {
		try {
			String ipv4 = in.readUTF();
			int port = in.readInt();

			SlamInfo slamInfo = new SlamInfo();

			slamInfo.setIpv4(ipv4);
			slamInfo.setPort(port);

			logger.info("SLAM_REGISTER_DS, SLAM IP: " + ipv4 + " SLAM Port: " + port);

			DirectoryServer directoryServer = DirectoryServer.getInstance();
			directoryServer.setSlamInfo(slamInfo);

			out.writeByte(RapidMessages.OK); // errorCode
			out.flush();

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
	 * The function deals with the VMM_REGISTER_DS message. It registers the VMM
	 * to the DS.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void vmmRegisterDs(ObjectInputStream in, ObjectOutputStream out) {
		try {

			DirectoryServer directoryServer = DirectoryServer.getInstance();
			SlamInfo slamInfo = directoryServer.getSlamInfo();

			if (slamInfo == null) {
				out.writeByte(RapidMessages.ERROR); // errorCode
				out.flush();
				return;
			}

			String ipv4 = in.readUTF();
			int mactype = in.readInt();
			int freecpu = in.readInt();
			int cpunums = in.readInt();
			long freemem = in.readLong();
			int freegpu = in.readInt();
			int gpunums = in.readInt();
			String availtypes = in.readUTF();

			VmmInfo vmmInfo = DSManager.getVmmInfoByIp(ipv4);

			if (vmmInfo != null) {
				out.writeByte(RapidMessages.ERROR); // errorCode
				out.flush();
				return;
			}

			vmmInfo = new VmmInfo();

			vmmInfo.setIpv4(ipv4);
			vmmInfo.setMactype(mactype);
			vmmInfo.setFreecpu(freecpu);
			vmmInfo.setCpunums(cpunums);
			vmmInfo.setFreemem(freemem);
			vmmInfo.setFreegpu(freegpu);
			vmmInfo.setGpunums(gpunums);
			vmmInfo.setAvailtypes(availtypes);

			long vmmId = DSManager.insertVmmInfo(vmmInfo);

			logger.info(
					"VMM_REGISTER_DS, returned SLAM IP: " + slamInfo.getIpv4() + " SLAM Port: " + slamInfo.getPort());

			out.writeByte(RapidMessages.OK); // errorCode
			out.writeLong(vmmId); // vmmId
			out.writeUTF(slamInfo.getIpv4());
			out.writeInt(slamInfo.getPort());
			out.flush();

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
	 * The function deals with the VMM_NOTIFY_DS message. It notifies the DS
	 * about free resource information.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void vmmNotifyDs(ObjectInputStream in, ObjectOutputStream out) {

		try {
			long vmmid = in.readLong();
			VmmInfo vmmInfo = DSManager.getVmmInfo(vmmid);

			if (vmmInfo != null) {
				vmmInfo.setFreecpu(in.readInt());
				vmmInfo.setFreemem(in.readLong());
				vmmInfo.setFreegpu(in.readInt());

				DSManager.updateVmmInfo(vmmInfo);
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
	 * Convert raw IP address to string.
	 *
	 * @param rawBytes
	 *            raw IP address.
	 * @return a string representation of the raw ip address.
	 */
	private String getIpAddress(byte[] rawBytes) {
		int i = 4;
		String ipAddress = "";
		for (byte raw : rawBytes) {
			ipAddress += (raw & 0xFF);
			if (--i > 0) {
				ipAddress += ".";
			}
		}

		return ipAddress;
	}

	/**
	 * The function deals with the AC_REGISTER_NEW_DS message. It receives the
	 * message from a new AC and sends a proper VMM IP list.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 * @param socket
	 */
	public void acRegisterNewDs(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
		try {
			long userid = in.readLong();
			int vcpuNum = in.readInt();
			int memSize = in.readInt();
			int gpuCores = in.readInt();

			logger.info("AC_REGISTER_NEW_DS, userId: " + userid + " vcpuNum: " + vcpuNum + " memSize: " + memSize
					+ " gpuCores: " + gpuCores);

			if (userid > 0) {
				VmInfo vmInfo = DSManager.getVmInfoByUserid(userid);

				if (vmInfo != null) {
					vmInfo.setOffloadstatus(VmInfo.OFFLOAD_DEREGISTERED);
					vmInfo.setVmstatus(VmInfo.VM_STOPPED);
					DSManager.updateVmInfo(vmInfo);

					// need to send DEREGISTERED message to VMM or AS.
					DirectoryServer directoryServer = DirectoryServer.getInstance();
					VmmInfo vmmInfo = DSManager.getVmmInfo(vmInfo.getVmmid());

					Socket vmmSocket = new Socket(vmmInfo.getIpv4(), directoryServer.getVMMPort());
					ObjectOutputStream vmmOut = new ObjectOutputStream(vmmSocket.getOutputStream());
					vmmOut.flush();

					vmmOut.writeByte(RapidMessages.DS_VM_DEREGISTER_VMM);

					vmmOut.writeLong(userid); // userid
					vmmOut.flush();

					vmmOut.close();
					vmmSocket.close();
				}
			}

			// find available machines
			DSEngine dsEngine = DSEngine.getInstance();

			DirectoryServer directoryServer = DirectoryServer.getInstance();
			// Find available machines
			// RapidUtils.sendAnimationMsg(directoryServer.getAnimationAddress(),
			// directoryServer.getAnimationServerPort(),
			// RapidMessages.AnimationMsg.DS_NEW_FIND_MACHINES.toString());

			ArrayList<String> ipList = dsEngine.findAvailMachines(vcpuNum, memSize, gpuCores);

			if (ipList.size() == 0) {
				out.writeByte(RapidMessages.ERROR);
				out.flush();
				return;
			}

			UserInfo userInfo = new UserInfo();

			InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
			userInfo.setIpv4(getIpAddress(addr.getAddress().getAddress()));

			long newUserid = DSManager.insertUserInfo(userInfo);

			// OK <ip List>
			// RapidUtils.sendAnimationMsg(directoryServer.getAnimationAddress(),
			// directoryServer.getAnimationServerPort(),
			// RapidMessages.AnimationMsg.DS_NEW_IP_LIST_AC.toString());

			out.writeByte(RapidMessages.OK);
			out.writeLong(newUserid);
			out.writeObject(ipList);
			out.writeUTF(directoryServer.getSlamInfo().getIpv4());
			out.writeInt(directoryServer.getSlamInfo().getPort());
			out.flush();

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
	 * The function deals with the AC_REGISTER_PREV_DS message. It receives the
	 * message from a existing AC and sends its VMM IP address.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 * @param socket
	 */
	public void acRegisterPrevDs(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
		try {
			long userid = in.readLong();

			/*
			 * int qosFlag = in.readInt(); String qosParam = ""; if (qosFlag ==
			 * 1) qosParam = in.readUTF();
			 */

			logger.info("AC_REGISTER_PREV_DS, userId: " + userid);

			UserInfo userInfo = null;

			if ((userInfo = DSManager.getUserInfo(userid)) == null) {
				out.writeByte(RapidMessages.ERROR);
				out.flush();
				return;
			}

			VmInfo vmInfo = DSManager.getVmInfoByUserid(userid);

			if (vmInfo == null || vmInfo.getOffloadstatus() == VmInfo.OFFLOAD_DEREGISTERED) {
				out.writeByte(RapidMessages.ERROR);
				out.flush();
				return;
			}

			// Find the previous machine
			DirectoryServer directoryServer = DirectoryServer.getInstance();
			// RapidUtils.sendAnimationMsg(directoryServer.getAnimationAddress(),
			// directoryServer.getAnimationServerPort(),
			// RapidMessages.AnimationMsg.DS_PREV_FIND_MACHINE.toString());

			VmmInfo vmmInfo = DSManager.getVmmInfo(vmInfo.getVmmid());

			if (vmmInfo == null) {
				out.writeByte(RapidMessages.ERROR);
				out.flush();
				return;
			}

			// QoS metric check is required later. TBD.
			InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
			userInfo.setIpv4(getIpAddress(addr.getAddress().getAddress()));

			/*
			 * if (qosFlag == 1) userInfo.setQosparam(qosParam);
			 */

			DSManager.updateUserInfo(userInfo);

			// OK <IP>
			// RapidUtils.sendAnimationMsg(directoryServer.getAnimationAddress(),
			// directoryServer.getAnimationServerPort(),
			// RapidMessages.AnimationMsg.DS_PREV_IP_AC.toString());

			ArrayList<String> ipList = new ArrayList<String>();
			ipList.add(vmmInfo.getIpv4());

			out.writeByte(RapidMessages.OK);
			out.writeLong(userid);
			out.writeObject(ipList);
			out.writeUTF(directoryServer.getSlamInfo().getIpv4());
			out.writeInt(directoryServer.getSlamInfo().getPort());
			out.flush();

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
	 * The function deals with the VM_REGISTER_DS message. It creates a new VM
	 * information structure.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void vmRegisterDs(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmmid = in.readLong();
			int category = in.readInt();
			int type = in.readInt();
			long userid = in.readLong();
			int vmstatus = in.readInt();

			VmInfo vmInfo = new VmInfo();

			vmInfo.setVmmid(vmmid);
			vmInfo.setCategory(category);
			vmInfo.setType(type);
			vmInfo.setUserid(userid);
			vmInfo.setVmstatus(vmstatus);

			long newVmid = DSManager.insertVmInfo(vmInfo);

			out.writeLong(newVmid);
			out.flush();
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
	 * The function deals with the VM_NOTIFY_DS message. It updates an existing
	 * VM information structure.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void vmNotifyDs(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();
			int vmstatus = in.readInt();

			VmInfo vmInfo = DSManager.getVmInfo(vmid);

			if (vmInfo != null) {
				vmInfo.setVmstatus(vmstatus);
				DSManager.updateVmInfo(vmInfo);
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
	 * The function deals with the HELPER_NOTIFY_DS message. It receives the
	 * message from the VMM when helper VMs launch.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void helperNotifyDs(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();
			String ipv4 = in.readUTF();

			VmInfo vmInfo = DSManager.getVmInfo(vmid);

			if (vmInfo != null) {
				vmInfo.setIpv4(ipv4);
				logger.info("helperNotifyDs: ipAddress:" + ipv4);
				DSManager.updateVmInfo(vmInfo);
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
	 * The function deals with the AS_RM_REGISTER_DS message. It receives the
	 * message from the AS when the AS starts.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 * @param socket
	 */
	public void asRmRegisterDs(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
		try {
			long userid = in.readLong();

			VmInfo vmInfo = DSManager.getVmInfoByUserid(userid);

			if (vmInfo == null) {
				out.writeByte(RapidMessages.ERROR);
				out.flush();
				return;
			}

			InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
			// logger.info("asRmRegisterDs: ipAddress:" +
			// addr.getAddress().getAddress());
			// vmInfo.setIpv4(getIpAddress(addr.getAddress().getAddress()));
			vmInfo.setOffloadstatus(VmInfo.OFFLOAD_REGISTERED);

			DSManager.updateVmInfo(vmInfo);

			out.writeByte(RapidMessages.OK);
			out.writeLong(vmInfo.getVmid());
			out.flush();
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
	 * The function deals with the FORWARD_REQ message. It receives the message
	 * from the AS when the AS requests forward execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 * @param socket
	 */
	public void forwardReq(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
		try {
			long vmid = in.readLong();

			VmInfo vmInfo = DSManager.getVmInfo(vmid);

			if (vmInfo == null || vmInfo.getCategory() == VmInfo.HELPER_VM) {
				ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();
				out.writeObject(vmList);
				out.flush();
				return;
			}

			VmmInfo vmmInfo = DSManager.getVmmInfo(vmInfo.getVmmid());

			if (vmmInfo == null) {
				ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();
				out.writeObject(vmList);
				out.flush();
				return;
			}

			ScheduleEntity scheduleEntity = new ScheduleEntity();
			scheduleEntity.setVmid(vmid);
			scheduleEntity.setNumRequest(1);
			scheduleEntity.setSocket(socket);
			scheduleEntity.setIn(in);
			scheduleEntity.setOut(out);

			synchronized (parallelQueue) {
				parallelQueue.add(scheduleEntity);
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
	 * The function deals with the FORWARD_START message. It receives the
	 * message from the AS when the AS starts forward execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void forwardStart(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();

			ArrayList<VmInfo> vmInfoList = null;
			synchronized (parallelMap) {
				vmInfoList = parallelMap.get(vmid);
			}

			if (vmInfoList == null)
				return;

			Iterator<VmInfo> vmInfoListIterator = vmInfoList.iterator();
			while (vmInfoListIterator.hasNext()) {
				VmInfo vmInfo = vmInfoListIterator.next();

				vmInfo.setOffloadstatus(VmInfo.OFFLOAD_OCCUPIED);
				DSManager.updateVmInfo(vmInfo);
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
	 * The function deals with the FORWARD_END message. It receives the message
	 * from the AS when the AS ends forward execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void forwardEnd(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();

			ArrayList<VmInfo> vmInfoList = null;
			synchronized (parallelMap) {
				vmInfoList = parallelMap.get(vmid);
			}

			if (vmInfoList == null)
				return;

			Iterator<VmInfo> vmInfoListIterator = vmInfoList.iterator();
			while (vmInfoListIterator.hasNext()) {
				VmInfo vmInfo = vmInfoListIterator.next();

				vmInfo.setOffloadstatus(VmInfo.OFFLOAD_RELEASED);
				DSManager.updateVmInfo(vmInfo);
			}

			synchronized (parallelMap) {
				parallelMap.remove(vmid);
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
	 * The function deals with the PARALLEL_REQ message. It receives the message
	 * from the AS when the AS requests parallel execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 * @param socket
	 */
	public void parallelReq(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
		try {
			long vmid = in.readLong();
			int number = in.readInt();

			logger.info("parallelReq vmid: " + vmid + " number: " + number);

			VmInfo vmInfo = DSManager.getVmInfo(vmid);

			if (vmInfo == null || vmInfo.getCategory() == VmInfo.HELPER_VM) {
				logger.info("parallelReq vmInfo == null || vmInfo.getCategory() == VmInfo.HELPER_VM");
				ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();
				out.writeObject(vmList);
				out.flush();
				return;
			}

			VmmInfo vmmInfo = DSManager.getVmmInfo(vmInfo.getVmmid());

			if (vmmInfo == null) {
				logger.info("parallelReq vmInfo == null");
				ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();
				out.writeObject(vmList);
				out.flush();
				return;
			}

			List<VmmInfo> vmmInfoList = DSManager.vmmInfoList();

			// every VMM has two helper VMs.
			if (vmmInfoList.size() * 2 < number) {
				logger.info("vmmInfoList.size() * 2 < number");
				ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();
				out.writeObject(vmList);
				out.flush();
				return;
			}

			ScheduleEntity scheduleEntity = new ScheduleEntity();
			scheduleEntity.setVmid(vmid);
			scheduleEntity.setNumRequest(number);
			scheduleEntity.setSocket(socket);
			scheduleEntity.setIn(in);
			scheduleEntity.setOut(out);

			synchronized (parallelQueue) {
				parallelQueue.add(scheduleEntity);
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
	 * The function deals with the PARALLEL_START message. It receives the
	 * message from the AS when the AS starts parallel execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void parallelStart(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();

			ArrayList<VmInfo> vmInfoList = null;
			synchronized (parallelMap) {
				vmInfoList = parallelMap.get(vmid);
			}

			if (vmInfoList == null)
				return;

			Iterator<VmInfo> vmInfoListIterator = vmInfoList.iterator();
			while (vmInfoListIterator.hasNext()) {
				VmInfo vmInfo = vmInfoListIterator.next();

				vmInfo.setOffloadstatus(VmInfo.OFFLOAD_OCCUPIED);
				DSManager.updateVmInfo(vmInfo);
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
	 * The function deals with the PARALLEL_END message. It receives the message
	 * from the AS when the AS ends parallel execution.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void parallelEnd(ObjectInputStream in, ObjectOutputStream out) {
		try {
			long vmid = in.readLong();

			ArrayList<VmInfo> vmInfoList = null;
			synchronized (parallelMap) {
				vmInfoList = parallelMap.get(vmid);
			}

			if (vmInfoList == null)
				return;

			Iterator<VmInfo> vmInfoListIterator = vmInfoList.iterator();
			while (vmInfoListIterator.hasNext()) {
				VmInfo vmInfo = vmInfoListIterator.next();

				vmInfo.setOffloadstatus(VmInfo.OFFLOAD_RELEASED);
				DSManager.updateVmInfo(vmInfo);
			}

			synchronized (parallelMap) {
				parallelMap.remove(vmid);
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
	 * The function deals with the DEMO_SERVER_REGISTER_DS message. It receives
	 * the message from the animation server when the the animation server
	 * starts.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void demoServerRegisterDs(ObjectInputStream in, ObjectOutputStream out) {
		DirectoryServer directoryServer = DirectoryServer.getInstance();

		try {
			directoryServer.setAnimationAddress(in.readUTF());
			logger.info("animationServerIp is: " + directoryServer.getAnimationAddress());

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
	 * The function deals with the GET_DEMO_SERVER_IP_DS message. It receives
	 * the message from RAPID components when they want to know the IP address
	 * of the animation server.
	 * 
	 * @param in
	 *            ObjectInputStream instance retrieved by the socket.
	 * @param out
	 *            ObjectOutputStream instance retrieved by the socket.
	 */
	public void getDemoServerIpDs(ObjectInputStream in, ObjectOutputStream out) {
		DirectoryServer directoryServer = DirectoryServer.getInstance();

		try {
			out.writeUTF(directoryServer.getAnimationAddress());
			out.flush();
		} catch (Exception e) {
			String message = "";
			for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
				message = message + System.lineSeparator() + stackTraceElement.toString();
			}
			logger.error("Caught Exception: " + e.getMessage() + System.lineSeparator() + message);
			e.printStackTrace();
		}
	}

	private ArrayList<String> findAvailMachines(int vcpuNum, int memSize, int gpuCores) {
		ArrayList<String> ipList = new ArrayList<String>();
		int maxCount = 5;

		List<VmmInfo> vmmInfoList = DSManager.vmmInfoListByLowUtil();
		Iterator<VmmInfo> vmmInfoListIterator = vmmInfoList.iterator();

		while (vmmInfoListIterator.hasNext()) {
			VmmInfo vmmInfo = vmmInfoListIterator.next();

			// CPU utilization check
			float desiredCpuUtilization = ((float) vcpuNum / (float) vmmInfo.getCpunums()) * 100;
			if ((int) desiredCpuUtilization > (100 - vmmInfo.getFreecpu()))
				continue;

			// memory amount check. Temporarily commented because
			// osBean.getFreePhysicalMemorySize() does not work well after the
			// kernel version 3.2.
			/*
			 * if ((memSize / 1024) > vmmInfo.getFreemem()) continue;
			 */

			// GPU utilization check
			boolean gpuSuccess = true;

			if (gpuCores != 0) {
				float desiredGpuUtilization = ((float) gpuCores / (float) vmmInfo.getGpunums()) * 100;
				if ((int) desiredGpuUtilization > (100 - vmmInfo.getFreegpu()))
					gpuSuccess = false;
			}

			if (gpuSuccess == false)
				continue;

			ipList.add(vmmInfo.getIpv4());
			maxCount--;

			if (maxCount == 0)
				return ipList;
		}
		return ipList;
	}

	/**
	 * This function finds appropriate helper VMs that are not occupied. *
	 * 
	 * @param vmid
	 *            VM ID of the requester.
	 * @param vmmIpv4
	 *            VMM IP address of the requester VM.
	 * @param exclude
	 *            If this value is true, it excludes the VMM IP address of the
	 *            requester VM.
	 * @param number
	 *            number of required helper VMs
	 * @return Helper VM IP list.
	 */
	public ArrayList<VmInfo> findHelperVMs(long vmid, String vmmIpv4, boolean exclude, int number) {
		ArrayList<VmInfo> vmList = new ArrayList<VmInfo>();

		List<VmmInfo> vmmInfoList = DSManager.vmmInfoListByLowUtil();
		Iterator<VmmInfo> vmmInfoListIterator = vmmInfoList.iterator();

		while (vmmInfoListIterator.hasNext()) {
			VmmInfo vmmInfo = vmmInfoListIterator.next();

			if (exclude == true && vmmInfo.getIpv4().equals(vmmIpv4))
				continue;

			List<VmInfo> vmInfoList = DSManager.helperVmInfoListByVmmid(vmmInfo.getVmmid());
			Iterator<VmInfo> vmInfoListIterator = vmInfoList.iterator();
			while (vmInfoListIterator.hasNext()) {
				VmInfo vmInfo = vmInfoListIterator.next();

				logger.info("in findHelperVMs, vmId: " + vmInfo.getVmid() + " vmInfo.getVmstatus: "
						+ vmInfo.getVmstatus() + " vmInfo.getOffloadstatus() " + vmInfo.getOffloadstatus());

				if (vmInfo.getVmstatus() == VmInfo.VM_STOPPED || vmInfo.getVmstatus() == VmInfo.VM_SUSPENDED)
					continue;

				if (vmInfo.getOffloadstatus() == VmInfo.OFFLOAD_OCCUPIED
						|| vmInfo.getOffloadstatus() == VmInfo.OFFLOAD_DEREGISTERED
						|| vmInfo.getOffloadstatus() == VmInfo.OFFLOAD_RESERVED)
					continue;

				vmList.add(vmInfo);

				if (vmList.size() == number) {
					synchronized (parallelMap) {
						parallelMap.put(vmid, vmList);
					}
					return vmList;
				}
			}
		}
		return vmList;
	}

	/**
	 * @return
	 */
	public Map<Long, ArrayList<VmInfo>> getParallelMap() {
		return parallelMap;
	}

	/**
	 * @param parallelMap
	 */
	public void setParallelMap(Map<Long, ArrayList<VmInfo>> parallelMap) {
		this.parallelMap = parallelMap;
	}

	/**
	 * @return
	 */
	public List<ScheduleEntity> getParallelQueue() {
		return parallelQueue;
	}

	/**
	 * @param parallelQueue
	 */
	public void setParallelQueue(List<ScheduleEntity> parallelQueue) {
		this.parallelQueue = parallelQueue;
	}
}

class ParallelTimerTask extends TimerTask {
	private Logger logger = Logger.getLogger(getClass());

	public void run() {
		DSEngine dsEngine = DSEngine.getInstance();

		List<ScheduleEntity> parallelQueue = dsEngine.getParallelQueue();

		synchronized (parallelQueue) {
			Iterator<ScheduleEntity> scheduleEntityIterator = parallelQueue.iterator();

			while (scheduleEntityIterator.hasNext()) {
				ScheduleEntity scheduleEntity = scheduleEntityIterator.next();

				// find some helper VMs.
				ArrayList<VmInfo> vmList = dsEngine.findHelperVMs(scheduleEntity.getVmid(), scheduleEntity.getIpv4(),
						false, scheduleEntity.getNumRequest());

				logger.info("in ParallelTimerTask vmList.size: " + vmList.size());

				if (vmList.size() == scheduleEntity.getNumRequest()) {
					ObjectOutputStream out = scheduleEntity.getOut();
					try {

						ArrayList<String> ipList = new ArrayList<String>();
						Iterator<VmInfo> vmListIterator = vmList.iterator();
						while (vmListIterator.hasNext()) {
							VmInfo vmInfo = vmListIterator.next();
							vmInfo.setOffloadstatus(VmInfo.OFFLOAD_RESERVED);
							DSManager.updateVmInfo(vmInfo);

							ipList.add(vmInfo.getIpv4());
						}

						// out.writeByte(RapidMessages.OK);
						out.writeObject(ipList);
						out.flush();

						scheduleEntity.getIn().close();
						scheduleEntity.getOut().close();
						scheduleEntity.getSocket().close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					scheduleEntityIterator.remove();
				} else {
					break;
				}
			}
		}
	}
}
