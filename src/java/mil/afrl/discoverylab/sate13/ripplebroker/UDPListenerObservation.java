package mil.afrl.discoverylab.sate13.ripplebroker;

import java.net.InetSocketAddress;

/**
 *
 * @author james
 */
public class UDPListenerObservation {

    private InetSocketAddress sender;
    private byte[] message;

    public UDPListenerObservation(InetSocketAddress sender, byte[] message) {
        this.sender = sender;
        this.message = message;
    }

    /**
     * @return the sender
     */
    public InetSocketAddress getSender() {
        return sender;
    }

    /**
     * @return the message
     */
    public byte[] getMessage() {
        return message;
    }
}
