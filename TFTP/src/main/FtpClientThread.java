package main;

import java.io.*;
import java.net.*;
import java.util.*;

public class FtpClientThread extends Thread {
	protected DatagramSocket clientSocket = null;
	//protected BufferedReader in = null;
	static int FTP_SERVER_PORT = 9093;
	static int FTP_CLIENT_PORT = 9094;
	public String fileName;
	Queue<FTPPacket> outBuffer;
	Queue<FTPPacket> tempBuffer;
	BufferedInputStream fileReader;
	long sequenceNumber=0;
	static int BUFFER_LENGTH =5;
	static int PACKET_DATA_LENGTH = 400;
	byte[] buffer;
	InetAddress serverAddress;
	InetAddress clientAddress;
	byte HAS = 1;
	byte HASNOT=0;
	//indicates that there is more data to read from file
	boolean moreFile = true;
	boolean isConnected = false;

	//constructor
	public FtpClientThread(InetAddress serverAddress, String fileName) {
		this.serverAddress=serverAddress;
		this.fileName = fileName;
		//creates socket bound to pre-specified client port
		createSocket (FTP_CLIENT_PORT);
		this.clientAddress=clientSocket.getLocalAddress();
		//buffer for FTPPacket data.
		outBuffer = new LinkedList<FTPPacket>();
		//temporary buffer, used to simplify queuing algorithm
		tempBuffer = new LinkedList<FTPPacket>();
		buffer = new byte[PACKET_DATA_LENGTH+12];
		//opens input file
		openInputFile();
	}

	//createSocket method.
	public void createSocket(int port) {
		try {
			//Client and server use different ports.
			this.clientSocket=new DatagramSocket(port);
			this.clientSocket.setSoTimeout(2000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//main logic
	public void run() {
		//this is a check for existing connection between server and client
		while (!isConnected) {
			try {
				//this method creates a "connection" over UDP
				connect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while ((moreFile)||(!(outBuffer.isEmpty()))) {
			queueData();
			sendAllPackets(); 
			//receives packet, gets sequence number of the packet
			long seq=receiveData();
			//deletes all packets from the queue with sequence number smaller than received number.
			dequeueData(seq);
		}
		clientSocket.close();
	}

	//increases sequence number
	public synchronized void increaseSequence(long offset) {
		sequenceNumber +=offset;
	}

	//sets sequence number
	public synchronized void setSequence(long number) {
		sequenceNumber=number;
	}

	//gets current sequence number
	public synchronized long getSequence() {
		return sequenceNumber;
	}

	//connects to server - sends SYN, waits for receive. Establishes a "connection"
	//This method is separate from other code, because it doesn't queue data
	public void connect() throws IOException {
		FTPPacket synPacket = new FTPPacket(sequenceNumber, clientAddress, HASNOT, HAS, HASNOT, 0, null);
		DatagramPacket sPacket = new DatagramPacket(synPacket.getBinary(), 12, serverAddress, FTP_SERVER_PORT);
		clientSocket.send(sPacket);
		//UDP header is 8 bytes, our FTP header is 12 bytes
		byte[] buf = new byte[20];
		DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
		clientSocket.receive(rPacket);
		FTPPacket synAckPacket = new FTPPacket(rPacket.getData(),rPacket.getLength());
		//if received packet has SYN and ACK flags set, and its sequence ==1
		if ((synAckPacket.isACK())&&(synAckPacket.isSYN())&&(synAckPacket.getSequenceNumber()==1)){
			isConnected=true;
			//if connected - sets sequence number to 1
			setSequence(1);
		}
	}

	//queues data from the file to the queue
	public synchronized void queueData() {
		//counter is a number of packets in the queue
		int counter = outBuffer.size();
		while (counter<BUFFER_LENGTH) {
			//if file has ended
			if (!moreFile) break;
			outBuffer.add(readPacket());
		}
	}

	//dequeues received packets with sequence numbers < specified sequence
	public synchronized void dequeueData(long sequence) {
		if (!outBuffer.isEmpty()) {
			FTPPacket packet = outBuffer.peek();
			long seq = packet.SequenceNumber;
			while ((seq<sequence)&&(!(outBuffer.isEmpty()))) {
				outBuffer.remove();
				packet=outBuffer.peek();
				seq=packet.SequenceNumber;
			}
		}
	}

	//gets POSIX timestamp in milliseconds
	public long getTime() {
		return System.currentTimeMillis();
	}

	//receives data, returns sequence number
	public long receiveData() {
		DatagramPacket dataPacket = new DatagramPacket(buffer, 12);
		try {
			clientSocket.setSoTimeout(2000);
			clientSocket.receive(dataPacket);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		FTPPacket packet = new FTPPacket(dataPacket.getData(), dataPacket.getLength());
		if (packet.isACK()){
			return packet.getSequenceNumber();
		}
		else return -1;
	}

	//reads data from file, creates FTPPacket
	public synchronized FTPPacket readPacket() {
		//since fileName.length() can be only up to 255 characters
		byte filenameLength = (byte)fileName.length();
		byte[] payload = new byte[PACKET_DATA_LENGTH];
		int res=0;
		//byte[] buffer = new byte [PACKET_DATA_LENGTH];
		//first packet
		if (getSequence()==1) {
			payload[0]=filenameLength;
			for (int i=1;i<=filenameLength;i++) {
				payload[i]=(byte) fileName.charAt(i-1);
			}
			//counter - how many bytes read
			int cnt=0;
			//fin flag
			byte fin = 0;
			for (cnt = 0;cnt<PACKET_DATA_LENGTH-filenameLength;cnt++) {
				try {
					res = fileReader.read(); //(payload, filenameLength+1, PACKET_DATA_LENGTH-filenameLength-1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//res equals to -1 only if end of file is reached
				if (res==-1) {
					fin=1;
					moreFile=false;
					break;
				}
				else {
					byte readByte = (byte)res;
					payload[cnt+filenameLength]=readByte;
				}
			}
			FTPPacket pack =new FTPPacket(getSequence(), clientAddress, HASNOT, HASNOT, fin, cnt+filenameLength, payload); 
			setSequence(cnt+1);
			return 	pack;
		}
		//next packets
		else {
			//counter - how many bytes read
			int cnt=0;
			//fin flag
			byte fin = 0;
			for (cnt = 0;cnt<PACKET_DATA_LENGTH;cnt++) {
				try {
					res = fileReader.read(); 
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//res equals to -1 only if end of file is reached
				if (res==-1) {
					fin=1;
					moreFile=false;
					break;
				}
				else {
					byte readByte = (byte)res;
					payload[cnt]=readByte;
				}
			}
			FTPPacket pack =  new FTPPacket(getSequence(), clientAddress, HASNOT, HASNOT, fin, cnt, payload);
			setSequence(cnt+1);
			return pack;
		}
	}

	//opens input file for reading
	private void openInputFile() {
		try {
			fileReader = new BufferedInputStream(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			moreFile=false;
			e.printStackTrace();
		}
	}

	//gets packet from the queue without deleting
	public synchronized FTPPacket getPacketFromQueue() {
		return outBuffer.peek();
	}

	//sends all packets from the queue
	public void sendAllPackets() {
		//get first packet in the queue
		FTPPacket ftpPacket = outBuffer.peek();
		long timeDiff = getTime()-ftpPacket.getTimestamp();
		tempBuffer.clear();
		//if timeout or first packet is new - resend all packets from the queue

		if ((timeDiff>2000)||(ftpPacket.getTimestamp()==0)) {
			//navigates the queue, sends data, puts updated packets in temporary queue
			for (FTPPacket packet: outBuffer) {
				packet=sendPacket(packet);
				tempBuffer.add(packet);
			}
			outBuffer.clear();
			//puts data from the temporary queue back in the original queue
			for (FTPPacket packet: tempBuffer) {
				outBuffer.add(packet);
			}
		}
		else {
			//still time left 
			tempBuffer.clear();
			for (FTPPacket packet:outBuffer) {
				//send only new packets
				if (packet.getTimestamp()==0) {
					packet=sendPacket(packet);
					tempBuffer.add(packet);
				}
				else {
					//even if packet wasn't updated - put it back to the queue to preserve initial order
					tempBuffer.add(packet);
				}
			}
			//put packets in the original buffer
			outBuffer.clear();
			for (FTPPacket packet:outBuffer){
				outBuffer.add(packet);
			}
		}
	}

	//sends FTPPackets
	public FTPPacket sendPacket(FTPPacket packet) {
		DatagramPacket dataPacket = new DatagramPacket(packet.getBinary(), packet.Length);
		try {
			clientSocket.send(dataPacket);
			packet.setTimestamp(getTime()); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return packet;
	}
}