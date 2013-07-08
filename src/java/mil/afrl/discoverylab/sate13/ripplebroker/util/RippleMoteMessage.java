package mil.afrl.discoverylab.sate13.ripplebroker.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mil.afrl.discoverylab.sate13.ripplebroker.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.SENSOR_TYPES;
import org.apache.log4j.Logger;

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
    private List<RippleData> data;
    private static Logger log = Logger.getLogger(Config.LOGGER_NAME);
    // Index constants
    private static final int INDEX_TIMESTAMP_START = 1;
    private static final int INDEX_TIMESTAMP_END = 4;
    private static final int INDEX_SENSOR_TYPE = 5;
    private static final int INDEX_OVERFLOW_COUNT = 0;
    private static final int INDEX_SAMPLE_COUNT = 6;
    private static final int INDEX_PULSE_START = 7;
    private static final int INDEX_PULSE_END = 8;
    private static final int INDEX_BLOOD_OX = 9;
    private static final int INDEX_ECG_START = 8;
    private static final int INDEX_ECG_OFFSET = 7;
    private static final int INDEX_TEMPERATURE = 7;
    // Size (in bytes) of data
    private static final int SIZE_OVERFLOW_COUNT = 1;
    private static final int SIZE_TIMESTAMP = 4;
    private static final int SIZE_SENSOR_TYPE = 1;
    private static final int SIZE_SAMPLE_COUNT = 1;
    private static final int SIZE_PULSE = 2;
    private static final int SIZE_BLOOD_OX = 1;
    private static final int SIZE_ECG_OFFSET = 1;
    private static final int SIZE_ECG_DATA = 2;
    private static final int SIZE_TEMPERATURE = 1;

    public static RippleMoteMessage parse(UDPListenerObservation obs) {

        RippleMoteMessage result = new RippleMoteMessage();
        byte[] message = obs.getMessage();
        List<RippleData> tData = new ArrayList<RippleData>();

        int overflowCount = (message[INDEX_OVERFLOW_COUNT] & 0xff);
        long timestamp = 0;
        int pulse = 0;
        int bloodOx = 0;
        int temperature = 0;

        result.senderAddress = obs.getSender();
        result.overflowCount = overflowCount;

        for (int i = INDEX_TIMESTAMP_START; i < INDEX_TIMESTAMP_END; i++) {
            timestamp |= (message[i] & 0xff);
            timestamp = (timestamp << 8);
        }
        timestamp |= (message[INDEX_TIMESTAMP_END] & 0xff);

        result.timestamp = timestamp;
        result.systemTime = obs.getReceiveTime();

        log.debug("Overflow count: " + overflowCount);
        log.debug("Timestamp: " + timestamp);

        int type = (message[INDEX_SENSOR_TYPE] & 0xff);

        if (type == Reference.SENSOR_TYPES.SENSOR_PULSE_OX.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_PULSE_OX;
            // got pulse and blood oxygen message
            int numPulseOxSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);

            // heart rate
            log.debug("Reported pulse and blood oxygen:");
            log.debug("Num samples: " + numPulseOxSamples);


            // iterate through multiple samples(if provided)
            for (int i = 0, j = INDEX_PULSE_START; i < numPulseOxSamples; i++, j += SIZE_PULSE + SIZE_BLOOD_OX) {
                pulse |= (message[j] & 0xff);
                pulse = (pulse << 8) | (message[j + 1] & 0xff);

                // blood oxygen
                bloodOx = (message[j + 2] & 0xff);

                // add point to list
                tData.add(new PulseOxData(pulse, bloodOx));
                log.debug("Pulse (BPM): " + pulse);
                log.debug("Blood oxygen: " + bloodOx);
            }


        } else if (type == Reference.SENSOR_TYPES.SENSOR_TEMPERATURE.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_TEMPERATURE;
            // got temperature reading message
            int numTemperatureSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);

            log.debug("Reported temperature:");
            log.debug("Num samples: " + numTemperatureSamples);
            // iterate through multiple samples(if provided)
            for (int i = 0, j = INDEX_TEMPERATURE; i < numTemperatureSamples; i++, j += SIZE_TEMPERATURE) {
                temperature = (message[j] & 0x00ff);

                // Add point to list
                tData.add(new TemperatureData(temperature));
                log.debug("Temperature: " + temperature);
            }


        } else if (type == Reference.SENSOR_TYPES.SENSOR_ECG.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_ECG;
            // got ecg reading message
            int numEcgSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);

            int sampleOffsets = (message[INDEX_ECG_OFFSET] & 0xff);
            int[] data = new int[numEcgSamples];

            log.debug("Reported ECG:");
            log.debug("Offset is " + sampleOffsets + " ms");

            for (int i = 0, buf_count = INDEX_ECG_START; i < numEcgSamples; i++, buf_count += SIZE_ECG_DATA) {
                data[i] |= (message[buf_count] & 0xff);
                data[i] = (data[i] << 8) | (message[buf_count + 1] & 0xff);

                tData.add(new ECGData(sampleOffsets, data[i]));
                log.debug("Data: " + data[i]);
            }



        } else {
            log.error("Unknown message! Type: " + message[5]);
        }
        result.data = tData;
        return result;
    }

    /**
     * @return the senderAddress
     */
    public InetSocketAddress getSenderAddress() {
        return senderAddress;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return the overflowCount
     */
    public int getOverflowCount() {
        return overflowCount;
    }

    /**
     * @return the systemTime
     */
    public Date getSystemTime() {
        return systemTime;
    }

    /**
     * @return the sensorType
     */
    public Reference.SENSOR_TYPES getSensorType() {
        return sensorType;
    }

    /**
     * @return the data
     */
    public List<RippleData> getData() {
        return data;
    }

    // Container classes for data points
    public interface RippleData {
    };

    public static class PulseOxData implements RippleData {

        public int pulse;
        public int bloodOxygen;

        public PulseOxData(int pulse, int bloodOx) {
            this.pulse = pulse;
            this.bloodOxygen = bloodOx;
        }
    }

    public static class TemperatureData implements RippleData {

        public int temperature;

        public TemperatureData(int temperature) {
            this.temperature = temperature;
        }
    }

    public static class ECGData implements RippleData {

        public int adcReading;
        public int sampleOffsets;

        public ECGData(int sampleOffsets, int adcReading) {
            this.adcReading = adcReading;
            this.sampleOffsets = sampleOffsets;
        }
    }
}
