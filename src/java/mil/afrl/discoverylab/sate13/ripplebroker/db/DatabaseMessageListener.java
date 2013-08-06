package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripple.data.model.MultiValueVital;
import mil.afrl.discoverylab.sate13.ripple.data.model.Patient;
import mil.afrl.discoverylab.sate13.ripple.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TYPES;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.ECGData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.PulseOxData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.RippleData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.TemperatureData;
import org.apache.log4j.Logger;

/**
 * Class to automatically update database based on updates from observable
 * listeners
 *
 * @author james
 */
public class DatabaseMessageListener implements Observer {

    // DB helper for this class
    private final DatabaseHelper databaseHelper;
    // Logger
    private final Logger log = Logger.getLogger(Config.LOGGER_NAME);

    public DatabaseMessageListener() {
        this.databaseHelper = DatabaseHelper.getInstance(null);
    }

    @Override
    public void update(Observable o, Object arg) {
//        log.debug("DatabaseMessage update called");
        RippleMoteMessage msg = null;
        // check this is a UDP listener observation
        if (arg instanceof UDPListenerObservation) {
            UDPListenerObservation obs = (UDPListenerObservation) arg;
            // attempt parse of observation data
            msg = RippleMoteMessage.parse(obs);
        } else if (arg instanceof RippleMoteMessage) {
            msg = (RippleMoteMessage) arg;
        } else {
            log.debug("Unknown object observed: " + arg.getClass().getName());
            return;
        }

        if (msg != null) {

            //List<Entry<Reference.TableColumns, String>> dataCols = new ArrayList<Entry<Reference.TableColumns, String>>();

            // verify that patient exists
            InetSocketAddress srcAddr = msg.getSenderAddress();
            if (!databaseHelper.patientExists(srcAddr.getAddress())) {
                // insert patient into database
                Patient p = new Patient(srcAddr.getAddress().getHostAddress());
                databaseHelper.insertRow(Reference.TABLE_NAMES.PATIENT, p.toListEntries());

                //dataCols.add((new SimpleEntry<Reference.TableColumns, String>(PATIENT_TABLE_COLUMNS.IP_ADDR, srcAddr.getAddress().getHostAddress())));
                //databaseHelper.insertRow(Reference.TABLE_NAMES.PATIENT, dataCols);
                //dataCols.clear();
            }
            // get their id
//            Vital v = new Vital();
//            v.pid = this.databaseHelper.getPatientId(srcAddr.getAddress());
//            v.server_timestamp = msg.getSystemTime();
//            v.sensor_timestamp = msg.getTimestamp();
//            v.sensor_type = "" + msg.getSensorType().getValue();

            MultiValueVital vital = new MultiValueVital();
            vital.pid = this.databaseHelper.getPatientId(srcAddr.getAddress());
            vital.server_timestamp = msg.getSystemTime();
            vital.sensor_timestamp = msg.getTimestamp();
            vital.sensor_type = msg.getSensorType().getValue();


            databaseHelper.bufferPatient(vital.pid);

            List<RippleData> data = msg.getData();
            int counter = 0;

            // Determine data type
            switch (msg.getSensorType()) {
                case SENSOR_PULSE_OX:

                    int[] sp02Values = new int[data.size()];
                    int[] pulseValues = new int[data.size()];
                    // TODO: assume only one value per message for now
                    for (RippleData value : data) {

                        // Set timestamp for samples
                        vital.sensor_timestamp = ((PulseOxData) value).sampleTime;

                        vital.value_type = VITAL_TYPES.VITAL_PULSE.getValue();
                        pulseValues[counter] = ((PulseOxData) value).pulse;
                        vital.period_ms = msg.getPeriodMs();
                        vital.values = pulseValues;
                        
                        databaseHelper.bufferMultiValueVital(vital);
                        this.databaseHelper.insertVitalBlob(vital.pid, vital.server_timestamp, vital.sensor_timestamp, vital.sensor_type, vital.value_type, vital.period_ms, pulseValues);


//                        databaseHelper.bufferVital(v);
//                        databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());

                        vital.value_type = VITAL_TYPES.VITAL_BLOOD_OX.getValue();
                        sp02Values[counter] = ((PulseOxData) value).bloodOxygen;
                        vital.values = sp02Values;
                        
                        databaseHelper.bufferMultiValueVital(vital);

//                        databaseHelper.bufferVital(v);
//                        databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());
                        this.databaseHelper.insertVitalBlob(vital.pid, vital.server_timestamp, vital.sensor_timestamp, vital.sensor_type, vital.value_type, vital.period_ms, sp02Values);

                    }
                    break;
                case SENSOR_ECG:

                    // Initialize some strings that are the same for all rows
                    vital.server_timestamp = msg.getSystemTime();
                    vital.sensor_type = msg.getSensorType().getValue();
                    vital.value_type = VITAL_TYPES.VITAL_ECG.getValue();
                    vital.sensor_timestamp = ((ECGData) data.get(0)).sampleTime;
                    vital.period_ms = msg.getPeriodMs();

                    // array for blob values
                    int[] values = new int[data.size()];
                    counter = 0;

                    // input entries
                    for (RippleData value : data) {
//                        v.sensor_timestamp = ((ECGData) value).sampleTime;
//                        v.value = ((ECGData) value).adcReading;
//                        databaseHelper.bufferVital(v);

                        values[counter] = ((ECGData) value).adcReading;//v.value;
                        counter++;

                        // TODO: convert ADC value to mV? Where?
                    }
                    vital.values = values;
                    this.databaseHelper.bufferMultiValueVital(vital);
                    
                    // TODO: remove hardcoded period
//                    final int ecgPeriodMs = msg.getPeriodMs();
                    this.databaseHelper.insertVitalBlob(vital.pid, vital.server_timestamp, vital.sensor_timestamp, vital.sensor_type, vital.value_type, vital.period_ms, values);
                    break;
                case SENSOR_TEMPERATURE:
                    vital.value_type = VITAL_TYPES.VITAL_TEMPERATURE.getValue();
                    vital.sensor_timestamp = ((TemperatureData) data.get(0)).sampleTime;
                    vital.period_ms = msg.getPeriodMs();

                    int[] temperatureValues = new int[data.size()];
                    counter = 0;
                    
                    for (RippleData value : data) {
//                        vital.value = ((TemperatureData) value).temperature;
                        temperatureValues[counter] = ((TemperatureData) value).temperature;
                        counter++;
                        
//                        v.sensor_timestamp = ((TemperatureData) value).sampleTime;
//                        databaseHelper.bufferVital(v);
//                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());
                    }
                    this.databaseHelper.bufferMultiValueVital(vital);
                    this.databaseHelper.insertVitalBlob(vital.pid, vital.server_timestamp, vital.sensor_timestamp, vital.sensor_type, vital.value_type, vital.period_ms, temperatureValues);
                    
                    break;
                default:
                    throw new AssertionError(msg.getSensorType().name());
            }
        }

//        log.debug("DatabaseMessage update finished");
    }
}
