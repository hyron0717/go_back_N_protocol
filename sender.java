import java.io.*;
import java.net.*;

public class sender {
	@SuppressWarnings("resource")
	public static void main(String argv[]) throws Exception{
		// check the number of arguments
		if(argv.length != 4){
			System.out.print("should have 4 arguments");
			System.exit(0);
		}
		
		// get input arguments
		String host_address = argv[0];
		InetAddress hostIP = InetAddress.getByName(host_address);
		int out_port = Integer.parseInt(argv[1]);
		int in_port = Integer.parseInt(argv[2]);
		DatagramSocket senderSocket = new DatagramSocket(in_port);
		String input_file = argv[3];
		
		// setup
		int max_length = 500;
		int max_window_size = 10;
		int seqnum_mod = 32;
		int timeout = 200;
		
		// init
		int windows = 0;
		int seq_num = 0;
		int last_ack_seq = -1;
		int last_ack_position = 0;
		int final_ack = -2;
		int position = 0;
		int skip_char;
		int check;
		int different = 0;
		String data;
		
		// output the seq num to seqnum.log
		FileOutputStream seqnum_log = new FileOutputStream(new File("seqnum.log"));
		OutputStreamWriter seqlog = new OutputStreamWriter(seqnum_log);
		
		// output the ack to ack.log
		FileOutputStream ack_log = new FileOutputStream(new File("ack.log"));
		OutputStreamWriter acklog = new OutputStreamWriter(ack_log);
		
		// input from the file
		FileInputStream inputfile = new FileInputStream(input_file);
		
		byte[] buffer = new byte[max_length];
		
		// loop until receive the last ack
		while(final_ack != last_ack_seq){
			// cannot send more than N=10 packets at a time
			while(windows < max_window_size){
				inputfile = new FileInputStream(input_file);
				skip_char = position * max_length;
				inputfile.skip(skip_char);
				int available = inputfile.available();
				buffer = new byte[max_length];
				check = inputfile.read(buffer, 0, max_length);
				
				// check if it is the last packet
				if(check == -1){
					final_ack = seq_num - 1;
					break;
				}
				else {
					// the last packet is less than 500 char
					if(available < max_length){
						seqlog.write(seq_num + "\n");
						buffer = new byte[available];
						inputfile = new FileInputStream(input_file);
						// skip the part sent before
						skip_char = position * max_length;
						inputfile.skip(skip_char);
						check = inputfile.read(buffer, 0, available);
						data = new String(buffer);
						packet nextpacket = packet.createPacket(seq_num, data);
						byte[] packet_UDP = nextpacket.getUDPdata();
						DatagramPacket sendPacket = new DatagramPacket(packet_UDP,packet_UDP.length,hostIP,out_port);
						senderSocket.send(sendPacket);
						
						seq_num = (seq_num + 1) % seqnum_mod;
						windows = windows + 1;
						position = position + 1;
					}
					// remain character is greater than 500 char, divide into 500 char packets
					else{
						seqlog.write(seq_num + "\n");
						data = new String(buffer);
						packet nextpacket = packet.createPacket(seq_num, data);
						byte[] packet_UDP = nextpacket.getUDPdata();
						DatagramPacket sendPacket = new DatagramPacket(packet_UDP, packet_UDP.length, hostIP, out_port);
						senderSocket.send(sendPacket);
						
						seq_num = (seq_num + 1) % seqnum_mod;
						windows = windows + 1;
						position = position + 1;
					}
				}
			}
			// set the timer to 200 ms
			senderSocket.setSoTimeout(timeout);
			
			while(true){
				try{
					byte[] receiveData = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
					senderSocket.receive(receivedPacket);
					packet ack_packet = packet.parseUDPdata(receiveData);
					int ack_num = ack_packet.getSeqNum();
					acklog.write(ack_num + "\n");
					
					//check if the ack are in order
					if(ack_num != last_ack_seq){
						// the receiver get the packet but the ack not in order or lost
						if(ack_num > last_ack_seq){
							different = ack_num - last_ack_seq;
						}
						else if ((ack_num < last_ack_seq) && (last_ack_seq + 10 > seqnum_mod - 1)){
							different = ack_num + seqnum_mod - last_ack_seq;
						}
						last_ack_position = last_ack_position + different;
						last_ack_seq = ack_num;
						windows = windows - different;
						break;
					}
				}
				// time out
				catch(SocketTimeoutException e){
					windows = 0;
					position = last_ack_position;
					seq_num = last_ack_seq + 1;
					break;
				}
			}
		}
		// set time out infinity
		senderSocket.setSoTimeout(0);
		
		//send the EOT packet
		packet EOTpacket = packet.createEOT(seq_num);
		byte [] packet_UDP = EOTpacket.getUDPdata();
		DatagramPacket sendPacket = new DatagramPacket(packet_UDP, packet_UDP.length, hostIP, out_port);
		senderSocket.send(sendPacket);
		
		// wait for the EOT packet from receiver
		while(true){
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			senderSocket.receive(receivePacket);
			packet eot_packet = packet.parseUDPdata(receiveData);
			// receive EOT acknowledgment
			if(eot_packet.getType()==2){
				break;
			}
		}
		
		seqlog.close();
		acklog.close();
		senderSocket.close();
		inputfile.close();
	}
}
