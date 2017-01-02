import java.io.*;
import java.net.*;

public class receiver {
	public static void main(String argv[]) throws Exception{
		if(argv.length != 4){
			System.out.print("should have 4 arguments");
			System.exit(0);
		}
		
		// get input arguments
		String host_address = argv[0];
		int out_port = Integer.parseInt(argv[1]);
		int in_port = Integer.parseInt(argv[2]);
		String output_file = argv[3];
		
		//init
		int waiting_seq = 0;
		int received_seq;
		
		DatagramSocket receiverSocket = new DatagramSocket(in_port);
		
		FileOutputStream output = new FileOutputStream(new File(output_file));
		OutputStreamWriter outputWriter = new OutputStreamWriter(output);
		
		// output arrival packet's seq num to arrival.log
		FileOutputStream arrival_log = new FileOutputStream(new File("arrival.log"));
		OutputStreamWriter arrivallog = new OutputStreamWriter(arrival_log);
		
		while(true){
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
			receiverSocket.receive(receivePacket);
			packet received_packet = packet.parseUDPdata(receiveData);
			received_seq = received_packet.getSeqNum();
			
			// receive EOT packet
			if(received_packet.getType()==2){
				packet EOT_packet = packet.createEOT(received_seq);
				byte[] packet_UDP = EOT_packet.getUDPdata();
				String temp = new String(packet_UDP);
				byte[] sendData = temp.getBytes();
				InetAddress hostIP = InetAddress.getByName(host_address);
				// send the EOT packet back to sender
				DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,hostIP,out_port);
				receiverSocket.send(sendPacket);
				break;
			}
			else{
				arrivallog.write(received_seq + "\n");
				
				// received expected packet
				if(received_seq == waiting_seq){
					outputWriter.write(new String(received_packet.getData()));
					packet ack = packet.createACK(waiting_seq);
					byte[] packet_UDP = ack.getUDPdata();
					String convert = new String(packet_UDP);
					byte[] sendData = convert.getBytes();
					InetAddress hostIP = InetAddress.getByName(host_address);
					DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,hostIP,out_port);
					receiverSocket.send(sendPacket);
					
					waiting_seq = (waiting_seq + 1) % 32;
				}
				// out of order
				else {
					if(waiting_seq !=0){
						packet ack = packet.createACK(waiting_seq-1);
						byte[] packet_UDP = ack.getUDPdata();
						String convert = new String(packet_UDP);
						byte[] sendData = convert.getBytes();
						InetAddress hostIP = InetAddress.getByName(host_address);
						DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,hostIP,out_port);
						receiverSocket.send(sendPacket);
					}
					// the seq is 32
					else{
						packet ack = packet.createACK(31);
						byte[] packet_UDP = ack.getUDPdata();
						String convert = new String(packet_UDP);
						byte[] sendData = convert.getBytes();
						InetAddress hostIP = InetAddress.getByName(host_address);
						DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,hostIP,out_port);
						receiverSocket.send(sendPacket);
					}
				}
			}
		}
		outputWriter.close();
		receiverSocket.close();
		arrivallog.close();
	}
}
