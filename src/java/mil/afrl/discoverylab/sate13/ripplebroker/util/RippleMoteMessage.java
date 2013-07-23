package mil.afrl.discoverylab.sate13.ripplebroker.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.SENSOR_TYPES;
import org.apache.log4j.Logger;

/**
 * Container for Ripple data from motes
 * @author james
 */
public class RippleMoteMessage {

    // Message information
    private InetSocketAddress senderAddress;
    private long timestamp;
    private int overflowCount;
    private Date systemTime;
    private Reference.SENSOR_TYPES sensorType;
    private List<RippleData> data;
    // Logger
    private static Logger log = Logger.getLogger(Config.LOGGER_NAME);
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
    private static final int SIZE_PULSE_OX_PERIOD = 2;
    private static final int SIZE_TEMPERATURE_PERIOD = 2;
    // Index constants
    private static final int INDEX_OVERFLOW_COUNT = 0;
    private static final int INDEX_TIMESTAMP_START = INDEX_OVERFLOW_COUNT + 1; // 1
    private static final int INDEX_TIMESTAMP_END = INDEX_TIMESTAMP_START + SIZE_TIMESTAMP - 1; // 4
    private static final int INDEX_SENSOR_TYPE = INDEX_TIMESTAMP_END + 1; // 5
    private static final int INDEX_SAMPLE_COUNT = INDEX_SENSOR_TYPE + 1; // 6
    private static final int INDEX_PULSE_OX_PERIOD = INDEX_SAMPLE_COUNT + 1; // 7
    private static final int INDEX_TEMPERATURE_PERIOD = INDEX_SAMPLE_COUNT + 1; // 7
    private static final int INDEX_ECG_PERIOD = INDEX_SAMPLE_COUNT + 1; // 7
    private static final int INDEX_PULSE_START = INDEX_PULSE_OX_PERIOD + 2; // 9
    private static final int INDEX_PULSE_END = INDEX_PULSE_START + SIZE_PULSE - 1; // 10
    private static final int INDEX_BLOOD_OX = INDEX_PULSE_END + 1; // 11
    private static final int INDEX_ECG_START = INDEX_ECG_PERIOD + 1; // 8
    private static final int INDEX_TEMPERATURE = INDEX_TEMPERATURE_PERIOD + 2; // 9

    /**
     * Parse listener observation to a RippleMoteMessage
     * @param obs
     * @return 
     */
    public static RippleMoteMessage parse(UDPListenerObservation obs) {

        RippleMoteMessage result = new RippleMoteMessage();
        byte[] message = obs.getMessage();
        List<RippleData> tData = new ArrayList<RippleData>();
        // NOTE: must bit-wise and message byte with 0xff or sign extension will occur and result in the wrong value
        int overflowCount = (message[INDEX_OVERFLOW_COUNT] & 0xff);
        long timestamp = 0;
        int pulse = 0;
        int bloodOx = 0;
        int temperature = 0;

        result.senderAddress = obs.getSender();
        result.overflowCount = overflowCount;
        // get timestamp (4 bytes unsigned)
        for (int i = INDEX_TIMESTAMP_START; i < INDEX_TIMESTAMP_END; i++) {
            timestamp |= (message[i] & 0xff);
            timestamp = (timestamp << 8);
        }
        timestamp |= (message[INDEX_TIMESTAMP_END] & 0xff);

        result.timestamp = timestamp;
        result.systemTime = obs.getReceiveTime();

        //log.debug("Overflow count: " + overflowCount);
        //log.debug("Timestamp: " + timestamp);
        
        // Get sensor type (1 byte unsigned)
        int type = (message[INDEX_SENSOR_TYPE] & 0xff);

        if (type == Reference.SENSOR_TYPES.SENSOR_PULSE_OX.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_PULSE_OX;
            // got pulse and blood oxygen message
            int numPulseOxSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);
            int spo2SamplePeriod = 0; 
            spo2SamplePeriod |= (message[INDEX_PULSE_OX_PERIOD] & 0x00ff);
            spo2SamplePeriod = (spo2SamplePeriod << 8) | (message[INDEX_PULSE_OX_PERIOD + 1] & 0x00ff);
            long tSampleTime;
            
            // heart rate
            //log.debug("Reported pulse and blood oxygen:");
            //log.debug("Num samples: " + numPulseOxSamples);
            //log.debug("Period is " + spo2SamplePeriod + " ms");

            // iterate through multiple samples(if provided)
            for (int i = 0, j = INDEX_PULSE_START; i < numPulseOxSamples; i++, j += SIZE_PULSE + SIZE_BLOOD_OX) {
                // reset variables for loop
                pulse = 0;
                bloodOx = 0;
                // pulse
                pulse |= (message[j] & 0xff);
                pulse = (pulse << 8) | (message[j + 1] & 0xff);

                // blood oxygen %
                bloodOx = (message[j + 2] & 0xff);
                // find samples actual time
                tSampleTime = (result.timestamp - (spo2SamplePeriod*(numPulseOxSamples - (i+1))));

                // add point to list
                tData.add(new PulseOxData(tSampleTime,pulse, bloodOx));
                //log.debug("Time: " + ((PulseOxData)tData.get(tData.size()-1)).sampleTime );
                //log.debug("Pulse (BPM): " + pulse);
                //log.debug("Blood oxygen: " + bloodOx);
            }


        } else if (type == Reference.SENSOR_TYPES.SENSOR_TEMPERATURE.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_TEMPERATURE;
            // got temperature reading message
            int numTemperatureSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);
            int temperaturePeriod = 0;
            temperaturePeriod |= (message[INDEX_TEMPERATURE_PERIOD] & 0x00ff);
            temperaturePeriod = (temperaturePeriod << 8) | (message[INDEX_TEMPERATURE_PERIOD + 1] & 0x00ff);
            long tSampleTime;

            //log.debug("Reported temperature:");
            //log.debug("Num samples: " + numTemperatureSamples);
            //log.debug("Period is " + temperaturePeriod + " ms");
            // iterate through multiple samples(if provided)
            for (int i = 0, j = INDEX_TEMPERATURE; i < numTemperatureSamples; i++, j += SIZE_TEMPERATURE) {
                
                // reset variables for loop
                temperature = 0;
                // get value
                temperature = (message[j] & 0x00ff);
                // find samples actual time
                tSampleTime = (result.timestamp - (temperaturePeriod*(numTemperatureSamples - (i+1))));
                
                // Add point to list
                tData.add(new TemperatureData(tSampleTime,temperature));
                
                //log.debug("Time1: " + tSampleTime);
                
                //log.debug("Time: " + ((TemperatureData)tData.get(tData.size()-1)).sampleTime + " Temperature: " + temperature);
            }


        } else if (type == Reference.SENSOR_TYPES.SENSOR_ECG.getValue()) {
            // Set sensor type
            result.sensorType = SENSOR_TYPES.SENSOR_ECG;
            // got ecg reading message
            int numEcgSamples = (message[INDEX_SAMPLE_COUNT] & 0x00ff);

            int sampleOffsets = (message[INDEX_ECG_PERIOD] & 0xff);
            int[] data = new int[numEcgSamples];
            long tSampleTime;

            //log.debug("Reported ECG:");
            //log.debug("Offset is " + sampleOffsets + " ms");
            // iterate through multiple readings
            for (int i = 0, buf_count = INDEX_ECG_START; i < numEcgSamples; i++, buf_count += SIZE_ECG_DATA) {
                // get adc value
                data[i] |= (message[buf_count] & 0xff);
                data[i] = (data[i] << 8) | (message[buf_count + 1] & 0xff);
                // find samples actual time
                tSampleTime = (result.timestamp - (sampleOffsets*(numEcgSamples - (i+1))));
                // add to data array
                tData.add(new ECGData(tSampleTime, sampleOffsets, data[i]));
                //log.debug("Time: " + ((ECGData)tData.get(tData.size()-1)).sampleTime + " Data: " + data[i]);
            }



        } else {
            log.error("Unknown message! Type: " + type);
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
        public long sampleTime;

        public PulseOxData(long sampleTime, int pulse, int bloodOx) {
            this.pulse = pulse;
            this.bloodOxygen = bloodOx;
            this.sampleTime = sampleTime;
        }
    }

    public static class TemperatureData implements RippleData {

        public int temperature;
        public long sampleTime;

        public TemperatureData(long sampleTime, int temperature) {
            this.temperature = temperature;
            this.sampleTime = sampleTime;
        }
    }

    public static class ECGData implements RippleData {

        public int adcReading;
        public int sampleOffsets;
        public long sampleTime;
        
        public ECGData(long sampleTime, int sampleOffsets, int adcReading) {
            this.adcReading = adcReading;
            this.sampleOffsets = sampleOffsets;
            this.sampleTime = sampleTime;
        }
    }
}
