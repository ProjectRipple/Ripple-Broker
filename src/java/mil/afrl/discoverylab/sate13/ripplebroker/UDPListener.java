/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mil.afrl.discoverylab.sate13.ripplebroker;

import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Observable;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class UDPListener extends Observable implements Runnable {

    private int listenPort;
    private InetAddress address;
    private DatagramSocket socket = null;
    private byte[] receiveBuffer;
    private DatagramPacket receivePacket;
    private static final int RECEIVE_BUF_SIZE = 400;
    private Logger log = Logger.getLogger(Reference.LOGGER_NAME);

    public UDPListener(InetAddress address, int port) {
        this.address = address;
        this.listenPort = port;
        this.setupSocket();

    }

    private void setupSocket() {
        try {
            // initialize socket(must do it this way for bind() to succeed)
            this.socket = DatagramChannel.open().socket();

            this.socket.setBroadcast(false);
            // Bind to local port
            this.socket.bind(new InetSocketAddress(this.address, this.listenPort));
            // initialize receive buffer
            this.receiveBuffer = new byte[RECEIVE_BUF_SIZE];
            // initialize receive packet
            this.receivePacket = new DatagramPacket(this.receiveBuffer, this.receiveBuffer.length);

            log.debug("Socket bound to " + this.socket.getLocalAddress().toString() + " port:" + this.socket.getLocalPort());
            log.debug("Receive buffer size: " + this.receiveBuffer.length);

        } catch (IOException ex) {
            log.error(ex);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Attempt receive
                this.socket.receive(this.receivePacket);
                // get sender info from socket address
                InetSocketAddress sockAddr = ((InetSocketAddress)this.receivePacket.getSocketAddress());
                // Print out info for debugging
                log.debug("Message from " + sockAddr.getHostString() + " (" + sockAddr.getAddress().toString() + ") " + " Port:" + sockAddr.getPort()) ;
                log.debug("Received (" + this.receivePacket.getLength() + ") " + new String(this.receivePacket.getData(), 0, this.receivePacket.getLength()));
                
                // Set this object as having changed
                this.setChanged();
                // Build notify argument object
                UDPListenerObservation notify = new UDPListenerObservation();
                notify.sender = sockAddr;
                notify.message = Arrays.copyOf(this.receivePacket.getData(), this.receivePacket.getLength());
                // Notify observers of new data
                this.notifyObservers(notify);
                
                // Reset packet length to buffer max
                this.receivePacket.setLength(this.receiveBuffer.length);
                // Reset packet buffer
                Arrays.fill(this.receiveBuffer, (byte)0);
            }
        } catch (IOException e) {
            log.debug(e);
        } finally {
            log.debug("Stopping run");
            this.socket.close();
            return;
        }

    }

    public void stop() {
        log.debug("Stopping MoteListener.");
        // close socket
        this.socket.close();
    }
}
