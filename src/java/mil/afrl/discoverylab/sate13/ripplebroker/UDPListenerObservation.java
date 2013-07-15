package mil.afrl.discoverylab.sate13.ripplebroker;

import java.net.InetSocketAddress;
import java.util.Date;

/**
 * Object representing a message received by the UDP Listener
 * @author james
 */
public class UDPListenerObservation {

    private InetSocketAddress sender;
    private Date receiveTime;
    private byte[] message;

    public UDPListenerObservation(InetSocketAddress sender, byte[] message, Date receiveTime) {
        this.sender = sender;
        this.message = message;
        this.receiveTime = receiveTime;
    }

    /**
     * @return the sender
     */
    public InetSocketAddress getSender() {
        return this.sender;
    }

    /**
     * @return the message
     */
    public byte[] getMessage() {
        return this.message;
    }
    
    /**
     * 
     * @return the receive time
     */
    public Date getReceiveTime()
    {
        return this.receiveTime;
    }
}
