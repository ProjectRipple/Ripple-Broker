package mil.afrl.discoverylab.sate13.ripplebroker.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.Observable;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import org.apache.log4j.Logger;

/**
 * Server to listen on a particular IP and port over UDP
 *
 * @author james
 */
public class UDPListener extends Observable implements Runnable {

    // Port server is listening on
    private int listenPort;
    // Address servier is listening on
    private InetAddress address;
    // Socket object that server is bound to
    private DatagramSocket socket = null;
    // Buffer for incoming messages
    private byte[] receiveBuffer;
    // Packet for incoming messages
    private DatagramPacket receivePacket;
    // size in bytes of receive buffer
    private static final int RECEIVE_BUF_SIZE = 400;
    // logger object
    private Logger log = Logger.getLogger(Config.LOGGER_NAME);

    public UDPListener(InetAddress address, int port) {
        this.address = address;
        this.listenPort = port;
        this.setupSocket();

    }

    /**
     * Initialize and bind to socket
     */
    private void setupSocket() {
        try {
            // initialize socket(must do it this way for bind() to succeed)
            this.socket = DatagramChannel.open().socket();
            // No using socket to broadcast
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
            // check if socket is null
            if(this.socket == null)
            {
                // run initialization if it is
                this.setupSocket();
            }
            while (true) {
                // Attempt receive (blocking call)
                this.socket.receive(this.receivePacket);
                // get sender info from socket address
                InetSocketAddress sockAddr = ((InetSocketAddress) this.receivePacket.getSocketAddress());
                // Print out info for debugging
//                log.debug("Message(" + this.receivePacket.getLength() + ") from " + sockAddr.getHostString() + " Port: " + sockAddr.getPort());
                //log.debug("Received (" + this.receivePacket.getLength() + ") " + new String(this.receivePacket.getData(), 0, this.receivePacket.getLength()));

                // Set this object as having changed
                this.setChanged();
                // Build notify argument object
                UDPListenerObservation notify = new UDPListenerObservation(sockAddr, Arrays.copyOf(this.receivePacket.getData(), this.receivePacket.getLength()), new Date());
                // Notify observers of new data
                this.notifyObservers(notify);
                log.debug("Notify finished");
                // Reset packet length to buffer max
                this.receivePacket.setLength(this.receiveBuffer.length);
                // Clear packet buffer
                //Arrays.fill(this.receiveBuffer, (byte) 0);
            }
        } catch (IOException e) {
            log.debug(e);
        } finally {
            log.debug("Stopping run");
            // Make sure socket gets closed
            this.stop();
        }

    }

    /**
     * Stop server run.
     */
    public void stop() {
        log.debug("Stopping UDPListener.");
        if(this.socket != null)
        {
            // close socket(will cause IOException and stop run)
            this.socket.close();
        }
        // reset socket object
        this.socket = null;
    }
}
