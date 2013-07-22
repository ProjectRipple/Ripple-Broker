package mil.afrl.discoverylab.sate13.ripplebroker.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class MulticastSendListener implements Observer, Runnable {

    // Logger
    private final Logger log = Logger.getLogger(Config.LOGGER_NAME);
    
    private InetAddress group;
    private MulticastSocket socket;
    
    public MulticastSendListener()
    {
        try {
            this.group = Inet6Address.getByName("ff02::1");
            this.socket = new MulticastSocket(1222);

            this.socket.joinGroup(new InetSocketAddress(this.group, 1222), NetworkInterface.getByName("wlan0"));
            
            log.debug("Network interface: " + this.socket.getNetworkInterface().getDisplayName());
        
        
        } catch (UnknownHostException ex) {
            log.error("UnknownHostException ", ex);
        } catch (IOException ex) {
            log.error("IOException MulticastSocket " , ex);
        }
        
    }
    
    @Override
    public void update(Observable o, Object arg) {
        RippleMoteMessage msg = null;
        // check this is a UDP listener observation
        if (arg instanceof UDPListenerObservation) {
            UDPListenerObservation obs = (UDPListenerObservation) arg;
            // attempt parse of observation data
            msg = RippleMoteMessage.parse(obs);
        } else {
            log.debug("Unknown object observed: " + arg.getClass().getName());
            return;
        }
    }

    @Override
    public void run() {
        
        String message = "{patient:{name:null,id:null},vital{pulse:22}}";
        
        DatagramPacket send = new DatagramPacket(message.getBytes(), message.length(), this.group, 1222);
        log.debug("MulticastSendListener run start");
        for(int i = 0; i < 100; i++)
        {
            try {
                log.debug("About to send on multicast socket");
                this.socket.send(send);
                log.debug("Test message sent");
                Thread.sleep(1000);
            } catch (IOException ex) {
                
                log.error("Failed to send, exiting", ex);
                return;
            } catch (InterruptedException ex) {
                log.error("My sleep was interrupted", ex);
            }
        }
        
    }
    
    public void stop()
    {
        if(this.socket != null)
        {
            this.socket.close();
        }
    }
}
