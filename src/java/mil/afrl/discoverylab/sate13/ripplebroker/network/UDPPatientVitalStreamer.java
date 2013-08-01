package mil.afrl.discoverylab.sate13.ripplebroker.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import mil.afrl.discoverylab.sate13.ripple.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import org.apache.log4j.Logger;

/**
 * Class to stream patient vital data for each patients to all subscribers of
 * that data over UDP
 *
 * @author james
 */
public class UDPPatientVitalStreamer {

    // Map of patients to subscriber lists
    private Map<Integer, List<InetSocketAddress>> subscriberMap = new HashMap<Integer, List<InetSocketAddress>>();
    // Map of patient to timestamp of last streamed data
    private Map<Integer, Long> lastSendMap = new HashMap<Integer, Long>();
    // Socket
    private DatagramSocket socket = null;
    // Timer for sending data
    private Timer sendTimer = null;
    // logger
    private static final Logger log = Logger.getLogger(Config.LOGGER_NAME);
    // database helper reference
    private DatabaseHelper dbhelper;
    // lock object
    private final Object mapLock = new Object();
    // constants
    private static final int SEND_PERIOD_MS = 250;
    // reference to instance
    private static final UDPPatientVitalStreamer instance = new UDPPatientVitalStreamer();

    private UDPPatientVitalStreamer() {
    }

    // underlying private methods for public interface
    private void addSubscriberP(Integer patient, InetSocketAddress subscriber) {
        synchronized (this.mapLock) {
            // Add patient if not in table
            if (!this.subscriberMap.containsKey(patient)) {
                this.subscriberMap.put(patient, new ArrayList<InetSocketAddress>());
                this.lastSendMap.put(patient, 0L);
            }
            // Only add each subscriber once
            if (!this.subscriberMap.get(patient).contains(subscriber)) {
                this.subscriberMap.get(patient).add(subscriber);
            }
        }
    }

    private void removeSubscriberP(Integer patient, InetSocketAddress subscriber) {
        synchronized (this.mapLock) {
            if (this.subscriberMap.containsKey(patient)) {
                this.subscriberMap.get(patient).remove(subscriber);
            }
        }
    }

    private void stopP() {
        // Stop timer
        if (this.sendTimer != null) {
            this.sendTimer.cancel();
            this.sendTimer = null;
        }
    }

    private void startP() {
        this.stopP();
        try {
            // Create socket
            if (this.socket == null) {
                this.socket = new DatagramSocket();
                this.socket.setBroadcast(false);
            }
            // Adding timer here because then we know socket was created
            this.sendTimer = new Timer();
            this.sendTimer.schedule(new sendTask(), 1000, SEND_PERIOD_MS);
            // get database helper
            if (this.dbhelper == null) {
                this.dbhelper = DatabaseHelper.getInstance(null);
            }

        } catch (SocketException ex) {
            log.error(this.getClass().getName() + ":Unable to create socket", ex);
        }
    }
    // Public static methods

    /**
     * Add subscriber to patient's feed
     *
     * @param patient Integer to subscribe to
     * @param subscriber Subscriber to add
     */
    public static void addSubscriber(Integer patient, InetSocketAddress subscriber) {
        instance.addSubscriberP(patient, subscriber);
    }

    /**
     * Remove subscriber from patient's feed
     *
     * @param patient Integer to unsubscribe from
     * @param subscriber Subscriber to remove
     */
    public static void removeSubscriber(Integer patient, InetSocketAddress subscriber) {
        instance.removeSubscriberP(patient, subscriber);
    }

    /**
     * Start sending vital data to subscribers
     */
    public static void start() {
        instance.startP();
    }

    /**
     * Stop sending data
     */
    public static void stop() {
        instance.stopP();
    }

    private class sendTask extends TimerTask {

        private final Vital[] referenceArray = new Vital[1];

        public sendTask() {
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            try {
                // Initialize local variables
                List<Vital> vitals = null;
                List<InetSocketAddress> subs = null;
                DatagramPacket sendPacket = new DatagramPacket(new byte[1], 1);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);
                Long lastSend = 0L;
                Long newLastSend = 0L;
                byte[] vitalsByteArray = null;
                // Iterate through current patients
                for (Integer p : subscriberMap.keySet()) {
                    // get subs for this patient
                    subs = subscriberMap.get(p);
                    // check that there is atleast 1 sub
                    if (!subs.isEmpty()) {
                        // get vitals for patient
                        lastSend = lastSendMap.get(p);
                        vitals = dbhelper.getBufferedVitalsForPatient(p, lastSend, 100, 0);
                        // set new last send timestamp
                        if (!vitals.isEmpty()) {

                            newLastSend = vitals.get(vitals.size() - 1).sensor_timestamp;
                            lastSendMap.remove(p);
                            lastSendMap.put(p, newLastSend);
                            // serialize objects
                            // write size first
                            oos.writeInt(vitals.size());
//                        for(Vital v : vitals){
//                            oos.writeObject(v);
//                        }
                            // write as an array of type Vital[]
                            oos.writeObject(vitals.toArray(this.referenceArray));
                            oos.flush();
                            // Get byte array of serialized objects
                            vitalsByteArray = baos.toByteArray();
                            // set packet data
                            sendPacket.setData(vitalsByteArray, 0, vitalsByteArray.length);
                            // send to all subs
                            for (InetSocketAddress sub : subs) {
                                // set socket for receiver
                                sendPacket.setSocketAddress(sub);
                                // send packet
                                socket.send(sendPacket);
                            }
                        }
                        // clear stream
                        baos.reset();

                    }

                }
            } catch (IOException ex) {
                log.error(this.getClass().getName() + ":Error writing object?", ex);
            } finally {
                try {
                    // close stream
                    if (oos != null) {
                        oos.close();
                    }
                } catch (IOException ex) {
                    log.error(this.getClass().getName() + ":Error closing object output stream", ex);
                }
            }
        }
    }
}
