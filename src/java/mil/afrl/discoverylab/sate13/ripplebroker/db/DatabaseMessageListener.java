package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.PATIENT_TABLE_COLUMNS;
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
        } else if(arg instanceof RippleMoteMessage) {
            msg = (RippleMoteMessage) arg;
        } else {
            log.debug("Unknown object observed: " + arg.getClass().getName());
            return;
        }

        if (msg != null) {

            List<Entry<Reference.TableColumns, String>> dataCols = new ArrayList<Entry<Reference.TableColumns, String>>();
            // verify that patient exists
            if (!this.databaseHelper.patientExists(msg.getSenderAddress().getAddress())) {
                // insert patient into database
                dataCols.add((new SimpleEntry<Reference.TableColumns, String>(PATIENT_TABLE_COLUMNS.IP_ADDR, msg.getSenderAddress().getAddress().getHostAddress())));
                this.databaseHelper.insertRow(Reference.TABLE_NAMES.PATIENT, dataCols);
                dataCols.clear();
            }
            // get their id
            // TODO: just use getPatientId as an exists method? (<0 = not exists?)
            int patientId = this.databaseHelper.getPatientId(msg.getSenderAddress().getAddress());
            // initalize columns in list
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.PID, "" + patientId));
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP, Reference.datetimeFormat.format(msg.getSystemTime())));
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.SENSOR_TYPE, "" + msg.getSensorType().getValue()));
            // save reference to these columns as they will change during below loop
            Entry<Reference.TableColumns, String> sensorTimestampEntry = new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP, "" + msg.getTimestamp());
            Entry<Reference.TableColumns, String> valueEntry = new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.VALUE, "");
            Entry<Reference.TableColumns, String> valueTypeEntry = new SimpleEntry<Reference.TableColumns, String>(VITAL_TABLE_COLUMNS.VALUE_TYPE, "");
            
            dataCols.add(sensorTimestampEntry);
            dataCols.add(valueEntry);
            dataCols.add(valueTypeEntry);

            List<RippleData> data = msg.getData();

            // Determine data type
            switch (msg.getSensorType()) {
                case SENSOR_PULSE_OX:
                    for (RippleData value : data) {
                        
                        // Set timestamp for samples
                        sensorTimestampEntry.setValue("" + ((PulseOxData) value).sampleTime);
                        
                        valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_PULSE.getValue());
                        valueEntry.setValue("" + ((PulseOxData) value).pulse);
                        
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, dataCols);

                        valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_BLOOD_OX.getValue());
                        valueEntry.setValue("" + ((PulseOxData) value).bloodOxygen);

                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, dataCols);

                    }
                    break;
                case SENSOR_ECG:
                    valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_ECG.getValue());
                    for (RippleData value : data) {
                        // TODO: convert ADC value to mV? Where?
                        valueEntry.setValue("" + ((ECGData) value).adcReading);
                        sensorTimestampEntry.setValue("" + ((ECGData) value).sampleTime);
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, dataCols);
                    }
                    break;
                case SENSOR_TEMPERATURE:
                    valueTypeEntry.setValue("" + VITAL_TYPES.VITAL_TEMPERATURE.getValue());
                    for (RippleData value : data) {
                        valueEntry.setValue("" + ((TemperatureData) value).temperature);
                        sensorTimestampEntry.setValue("" + ((TemperatureData) value).sampleTime);
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITAL, dataCols);
                    }
                    break;
                default:
                    throw new AssertionError(msg.getSensorType().name());
            }
        }
        
//        log.debug("DatabaseMessage update finished");
    }
}
