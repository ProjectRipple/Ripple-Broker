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
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Patient;
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Vital;
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
    private Map<Patient, List<InetSocketAddress>> subscriberMap = new HashMap<Patient, List<InetSocketAddress>>();
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

    private void UDPStreamer() {
    }

    // underlying private methods for public interface
    private void addSubscriberP(Patient patient, InetSocketAddress subscriber) {
        synchronized (this.mapLock) {
            // Add patient if not in table
            if (!this.subscriberMap.containsKey(patient)) {
                this.subscriberMap.put(patient, new ArrayList<InetSocketAddress>());
            }
            this.subscriberMap.get(patient).add(subscriber);
        }
    }

    private void removeSubscriberP(Patient patient, InetSocketAddress subscriber) {
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
     * @param patient Patient to subscribe to
     * @param subscriber Subscriber to add
     */
    public static void addSubscriber(Patient patient, InetSocketAddress subscriber) {
        instance.addSubscriberP(patient, subscriber);
    }

    /**
     * Remove subscriber from patient's feed
     *
     * @param patient Patient to unsubscribe from
     * @param subscriber Subscriber to remove
     */
    public static void removeSubscriber(Patient patient, InetSocketAddress subscriber) {
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

        public sendTask() {
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            try {
                // Initialize local variables
                List<Vital> vitals = null;
                List<InetSocketAddress> subs = null;
                DatagramPacket sendPacket = new DatagramPacket(null, 0);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);
                byte[] vitalsByteArray = null;
                // Iterate through current patients
                for (Patient p : subscriberMap.keySet()) {
                    // get subs for this patient
                    subs = subscriberMap.get(p);
                    // check that there is atleast 1 sub
                    if (!subs.isEmpty()) {
                        // get vitals for patient
                        vitals = dbhelper.getBufferedVitalsForPatient(p.pid, 0, 101, 0);
                        // serialize object
                        oos.writeObject(vitals);
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
