package mil.afrl.discoverylab.sate13.ripplebroker.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import static mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.SENSOR_TYPES.SENSOR_ECG;
import static mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.SENSOR_TYPES.SENSOR_PULSE_OX;
import static mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.SENSOR_TYPES.SENSOR_TEMPERATURE;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TYPES;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.RippleData;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class MulticastSendListener implements Observer, Runnable {

    // Logger
    private static final Logger log = Logger.getLogger(Config.LOGGER_NAME);
    private static final int MCAST_PORT = 1222;
    private InetAddress group;
    private MulticastSocket socket;
    private final DatabaseHelper databaseHelper;

    public MulticastSendListener() {
        this.databaseHelper = DatabaseHelper.getInstance(null);

        try {
            this.group = Inet6Address.getByName("ff02::1");
            this.socket = new MulticastSocket(1222);

            this.socket.joinGroup(new InetSocketAddress(this.group, MCAST_PORT), NetworkInterface.getByName("wlan0"));

            log.debug("Network interface: " + this.socket.getNetworkInterface().getDisplayName());


        } catch (UnknownHostException ex) {
            log.error("UnknownHostException ", ex);
        } catch (IOException ex) {
            log.error("IOException MulticastSocket ", ex);
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
        // Get patient ID from DB
        int patientId = this.databaseHelper.getPatientId(msg.getSenderAddress().getAddress());
        // Only care about latest reading, which also corresponds to sensor timestamp in message
        RippleData latestReading = msg.getData().get(msg.getData().size() - 1);
        // JSON array for vitals
        JsonArray vitals = new JsonArray();

        // Determine data type
        switch (msg.getSensorType()) {
            case SENSOR_PULSE_OX:
                // Only care about most recent data
                Vital pulse = new Vital(patientId, msg.getSystemTime(), msg.getTimestamp(),
                    msg.getSensorType().getValue() + "", VITAL_TYPES.VITAL_PULSE.getValue() + "",
                    ((RippleMoteMessage.PulseOxData) latestReading).pulse);

                vitals.add(new Gson().toJsonTree(pulse));

                Vital bloodOxygen = new Vital(patientId, msg.getSystemTime(), msg.getTimestamp(),
                    msg.getSensorType().getValue() + "", VITAL_TYPES.VITAL_BLOOD_OX.getValue() + "",
                    ((RippleMoteMessage.PulseOxData) latestReading).bloodOxygen);

                vitals.add(new Gson().toJsonTree(bloodOxygen));

                break;
            case SENSOR_ECG:
                // Don't set ecg this way
                break;
            case SENSOR_TEMPERATURE:
                Vital temperature = new Vital(patientId, msg.getSystemTime(), msg.getTimestamp(),
                    msg.getSensorType().getValue() + "", VITAL_TYPES.VITAL_TEMPERATURE.getValue() + "",
                    ((RippleMoteMessage.TemperatureData) latestReading).temperature);

                vitals.add(new Gson().toJsonTree(temperature));


                break;
            default:
                throw new AssertionError(msg.getSensorType().name());
        }

        
        // Do something if vitals were found
        if (vitals.size() > 0) {
            // Create root JSON object
            JsonObject json = new JsonObject();
            // Add vitals array to root object
            json.add("vitals", vitals);

            // Create send packet for multicast group
            DatagramPacket sendPacket = new DatagramPacket(json.toString().getBytes(), json.toString().length(), this.group, MCAST_PORT);
            // Send packet
            try {
                this.socket.send(sendPacket);
            } catch (IOException ex) {
                log.error("Failed to send", ex);
            }
        }

    }

    @Override
    public void run() {

        String message = "{patient:{name:null,id:null},vital{pulse:22}}";

        DatagramPacket send = new DatagramPacket(message.getBytes(), message.length(), this.group, MCAST_PORT);
        log.debug("MulticastSendListener run start");
        for (int i = 0; i < 100; i++) {
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

    public void stop() {
        if (this.socket != null) {
            this.socket.close();
        }
    }
}
