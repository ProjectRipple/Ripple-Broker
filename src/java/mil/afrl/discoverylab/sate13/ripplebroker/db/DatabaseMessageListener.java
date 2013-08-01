package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
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
            // TODO: just use getPatientId as an exists method? (<0 = not exists?)
            Vital v = new Vital();
            v.pid = this.databaseHelper.getPatientId(srcAddr.getAddress());
            v.server_timestamp = msg.getSystemTime();
            v.sensor_timestamp = msg.getTimestamp();
            v.sensor_type = "" + msg.getSensorType().getValue();

            databaseHelper.bufferPatient(v.pid);

            List<RippleData> data = msg.getData();

            // Determine data type
            switch (msg.getSensorType()) {
                case SENSOR_PULSE_OX:
                    for (RippleData value : data) {

                        // Set timestamp for samples
                        v.sensor_timestamp = ((PulseOxData) value).sampleTime;
                        //sensorTimestampEntry.setValue("" + ((PulseOxData) value).sampleTime);

                        v.value_type = "" + VITAL_TYPES.VITAL_PULSE.getValue();
                        //valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_PULSE.getValue());
                        v.value = ((PulseOxData) value).pulse;
                        //valueEntry.setValue("" + ((PulseOxData) value).pulse);

                        databaseHelper.bufferVital(v);
                        databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());

                        v.value_type = "" + VITAL_TYPES.VITAL_BLOOD_OX.getValue();
                        //valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_BLOOD_OX.getValue());
                        v.value = ((PulseOxData) value).bloodOxygen;
                        //valueEntry.setValue("" + ((PulseOxData) value).bloodOxygen);

                        databaseHelper.bufferVital(v);
                        databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());
                    }
                    break;
                case SENSOR_ECG:
//                    List<Map<Reference.TableColumns, String>> rows = new ArrayList<Map<Reference.TableColumns, String>>();
//                    Map<Reference.TableColumns, String> curRow;
//                    List<Reference.TableColumns> columns = new ArrayList<Reference.TableColumns>();

                    // Initialize some strings that are the same for all rows
                    v.server_timestamp = msg.getSystemTime();
                    v.sensor_type = "" + msg.getSensorType().getValue();
                    v.value_type = "" + VITAL_TYPES.VITAL_ECG.getValue();

//                    String pid = "" + v.pid;
//                    String serverTime = Reference.datetimeFormat.format(v.server_timestamp);

                    // Add columns to list
//                    columns.add(VITAL_TABLE_COLUMNS.PID);
//                    columns.add(VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP);
//                    columns.add(VITAL_TABLE_COLUMNS.SENSOR_TYPE);
//                    columns.add(VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP);
//                    columns.add(VITAL_TABLE_COLUMNS.VALUE);
//                    columns.add(VITAL_TABLE_COLUMNS.VALUE_TYPE);

                    // array for blob values
                    int[] values = new int[data.size()];
                    int counter = 0;
                    
                    // input entries
                    for (RippleData value : data) {
                        v.sensor_timestamp = ((ECGData) value).sampleTime;
                        v.value = ((ECGData) value).adcReading;
                        databaseHelper.bufferVital(v);

                        values[counter] = v.value;
                        counter++;
                        
                        // TODO: convert ADC value to mV? Where?
                        // Removed below because of change to blob
//                        curRow = new HashMap<Reference.TableColumns, String>();
//                        curRow.put(VITAL_TABLE_COLUMNS.PID, pid);
//                        curRow.put(VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP, serverTime);
//                        curRow.put(VITAL_TABLE_COLUMNS.SENSOR_TYPE, v.sensor_type);
//                        curRow.put(VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP, "" + v.sensor_timestamp);
//                        curRow.put(VITAL_TABLE_COLUMNS.VALUE, "" + v.value);
//                        curRow.put(VITAL_TABLE_COLUMNS.VALUE_TYPE, v.value_type);
//                        rows.add(curRow);
                    }
//                    this.databaseHelper.bulkInsert(Reference.TABLE_NAMES.VITAL, columns, rows);
                    
                    // TODO: remove hardcoded period
                    final int ecgPeriodMs = 5;
                    this.databaseHelper.insertVitalBlob(v.pid, msg.getSystemTime(), ((ECGData)data.get(0)).sampleTime, msg.getSensorType().getValue(), VITAL_TYPES.VITAL_ECG.getValue(), ecgPeriodMs, values);
                    break;
                case SENSOR_TEMPERATURE:
                    v.value_type = "" + VITAL_TYPES.VITAL_TEMPERATURE.getValue();
                    //valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_TEMPERATURE.getValue());
                    for (RippleData value : data) {
                        v.value = ((TemperatureData) value).temperature;
                        //valueEntry.setValue("" + ((TemperatureData) value).temperature);
                        v.sensor_timestamp = ((TemperatureData) value).sampleTime;
                        //sensorTimestampEntry.setValue("" + ((TemperatureData) value).sampleTime);
                        databaseHelper.bufferVital(v);
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());
                    }
                    break;
                default:
                    throw new AssertionError(msg.getSensorType().name());
            }
        }

//        log.debug("DatabaseMessage update finished");
    }
}
