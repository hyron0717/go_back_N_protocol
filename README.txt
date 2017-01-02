My assignement is implemented in Java.

How to run:
1.Type 'make' to compile sender.java and receiver.java

2.Run nEmulator

./nEmulator 
<emulator's receiving UDP port number in the forward (sender) direction>,
<receiver's network address>,
<receiver's receiving UDP port number>,
<emulator's receiving UDP port number in the backward (receiver) direction>,
<sender's network address>,
<sender's receiving UDP port number>,
<maximum delay of the link in units of millisecond>,
<packet discard probability>,
<verbose-mode> (Boolean: Set to 1, the network emulator will output its internal processing).

3.Run recevier

java receiver
<hostname for the network emulator> 
<UDP port number used by the link emulator to receive ACKs from the receiver>
<UDP port number used by the receiver to receive data from the emulator>
<name of the file into which the received data is written>

4.Run sender

java sender
<host address of the network emulator>
<UDP port number used by the emulator to receive data from the sender>
<UDP port number used by the sender to receive ACKs from the emulator>
<name of the file to be transferred>

5.The program will create seqnum.log, ack.log and arrival.log to show how the packet send and receive

6.Type 'make clean' to delete sender.class and receiver.class


-------------------------------------------------------------------------

Example Execution:
1. On the host host1: nEmulator 9991 host2 9994 9993 host3 9992 1 0.2 0
2. On the host host2: java receiver host1 9993 9994 <output File>
3. On the host host3: java sender host1 9991 9992 <input file>

-------------------------------------------------------------------------

Version:
javac -version: javac 1.8.0_91
make -v: GNU Make 3.81
hostname -f: ubuntu1404-002.student.cs.uwaterloo.ca
             ubuntu1404-004.student.cs.uwaterloo.ca
             ubuntu1404-006.student.cs.uwaterloo.ca
