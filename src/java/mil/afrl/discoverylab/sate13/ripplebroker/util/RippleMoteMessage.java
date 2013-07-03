package mil.afrl.discoverylab.sate13.ripplebroker.util;

import java.net.InetSocketAddress;
import java.util.Date;

/**
 *
 * @author james
 */
public class RippleMoteMessage {
    private InetSocketAddress senderAddress;
    private long timestamp;
    private int overflowCount;
    private Date systemTime;
    private Reference.SENSOR_TYPES sensorType;
        
}
