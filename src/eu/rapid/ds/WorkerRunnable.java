package eu.rapid.ds;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Logger;

import eu.project.rapid.common.RapidMessages;

public class WorkerRunnable implements Runnable {
	protected Socket clientSocket = null;
	protected String serverText = null;
	private Logger logger = Logger.getLogger(getClass());

	public WorkerRunnable(Socket clientSocket, String serverText) {
		this.clientSocket = clientSocket;
		this.serverText = serverText;
	}

	public void run() {
		try {
			DSEngine dsEngine = DSEngine.getInstance();

			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

			int command = (int) in.readByte();

			switch (command) {
			case RapidMessages.SLAM_REGISTER_DS:
				dsEngine.slamRegisterDs(in, out);
				break;
			case RapidMessages.VMM_REGISTER_DS:
				dsEngine.vmmRegisterDs(in, out);
				break;
			case RapidMessages.VMM_NOTIFY_DS:
				dsEngine.vmmNotifyDs(in, out);
				break;
			case RapidMessages.AC_REGISTER_NEW_DS:
				dsEngine.acRegisterNewDs(in, out, clientSocket);
				break;
			case RapidMessages.AC_REGISTER_PREV_DS:
				dsEngine.acRegisterPrevDs(in, out, clientSocket);
				break;
			case RapidMessages.VM_REGISTER_DS:
				dsEngine.vmRegisterDs(in, out);
				break;
			case RapidMessages.VM_NOTIFY_DS:
				dsEngine.vmNotifyDs(in, out);
				break;
			case RapidMessages.HELPER_NOTIFY_DS:
				dsEngine.helperNotifyDs(in, out);
				break;
			case RapidMessages.AS_RM_REGISTER_DS:
				dsEngine.asRmRegisterDs(in, out, clientSocket);
				break;
			case RapidMessages.FORWARD_REQ:
				dsEngine.forwardReq(in, out, clientSocket);
				break;
			case RapidMessages.FORWARD_START:
				dsEngine.forwardStart(in, out);
				break;
			case RapidMessages.FORWARD_END:
				dsEngine.forwardEnd(in, out);
				break;
			case RapidMessages.PARALLEL_REQ:
				dsEngine.parallelReq(in, out, clientSocket);
				break;
			case RapidMessages.PARALLEL_START:
				dsEngine.parallelStart(in, out);
				break;
			case RapidMessages.PARALLEL_END:
				dsEngine.parallelEnd(in, out);
				break;
				/*
			case RapidMessages.DEMO_SERVER_REGISTER_DS:
				dsEngine.demoServerRegisterDs(in, out);
				break;
			case RapidMessages.GET_DEMO_SERVER_IP_DS:
				dsEngine.getDemoServerIpDs(in, out);
				break;
				*/
			}

			/*
			 * in the case of FORWARD_REQ and PARALLEL_REQ, the socket is closed
			 * in the DS scheduler
			 */
			if (command != RapidMessages.FORWARD_REQ && command != RapidMessages.PARALLEL_REQ) {
				in.close();
				out.close();
				clientSocket.close();
			}

		} catch (IOException e) {
			String message = "";
			for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
				message = message + System.lineSeparator() + stackTraceElement.toString();
			}
			logger.error("Caught Exception: " + e.getMessage() + System.lineSeparator() + message);
			e.printStackTrace();
		}
	}
}