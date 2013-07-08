package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripplebroker.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.PATIENT_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITALS_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TYPES;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.ECGData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.PulseOxData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.RippleData;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.TemperatureData;
import org.apache.log4j.Logger;

/**
 * Class to automatically update database based on updates from observable listeners
 * @author james
 */
public class DatabaseMessageListener implements Observer {
    
    private final DatabaseHelper databaseHelper;
    private final Logger log = Logger.getLogger(Reference.LOGGER_NAME);

    public DatabaseMessageListener()
    {
        try {
            this.databaseHelper = DatabaseHelper.getInstance(null);
        } catch (ClassNotFoundException ex) {
            log.error("Error getting database helper instance", ex);
            throw new RuntimeException("Database helper not initialized.");
        }
    }
    
    @Override
    public void update(Observable o, Object arg) {
        RippleMoteMessage msg = null;
        if(arg instanceof UDPListenerObservation)
        {
            UDPListenerObservation obs = (UDPListenerObservation)arg;
            msg = RippleMoteMessage.parse(obs);
        }
        
        if(msg != null)
        {
            
            List<Entry<Reference.TableColumns, String>> dataCols = new ArrayList<Entry<Reference.TableColumns, String>>();
            // verify that patient exists
            if(!this.databaseHelper.patientExists(msg.getSenderAddress().getAddress()))
            {
                // insert patient into database
                dataCols.add((new SimpleEntry<Reference.TableColumns, String>(PATIENT_TABLE_COLUMNS.IP_ADDR, msg.getSenderAddress().getAddress().getHostAddress())));
                this.databaseHelper.insertRow(Reference.TABLE_NAMES.PATIENT, dataCols);
                dataCols.clear();
            }
            // get their id
            // TODO: just use getPatientId as an exists method? (<0 = not exists?)
            int patientId = this.databaseHelper.getPatientId(msg.getSenderAddress().getAddress());
            
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.PID, ""+patientId));
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.SERVER_TIMESTAMP, Reference.datetimeFormat.format(msg.getSystemTime())));
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.SENSOR_TIMESTAMP, ""+msg.getTimestamp()));
            dataCols.add(new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.SENSOR_TYPE, ""+msg.getSensorType().getValue()));
            
            Entry<Reference.TableColumns, String> valueEntry = new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.VALUE, "");
            Entry<Reference.TableColumns, String> valueTypeEntry = new SimpleEntry<Reference.TableColumns, String>(VITALS_TABLE_COLUMNS.VALUE_TYPE, "");
            
            dataCols.add(valueEntry);
            dataCols.add(valueTypeEntry);
            
            List<RippleData> data = msg.getData();
            
            switch(msg.getSensorType())
            {
                case SENSOR_PULSE_OX:
                    for(RippleData value: data)
                    {
                        valueTypeEntry.setValue(""+VITAL_TYPES.VITAL_PULSE.getValue());
                        valueEntry.setValue(""+((PulseOxData)value).pulse);
                        
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITALS, dataCols);
                        
                        valueTypeEntry.setValue(""+VITAL_TYPES.VITAL_BLOOD_OX.getValue());
                        valueEntry.setValue(""+((PulseOxData)value).bloodOxygen);
                        
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITALS, dataCols);
                        
                    }
                    break;
                case SENSOR_ECG:
                    valueTypeEntry.setValue(""+VITAL_TYPES.VITAL_ECG.getValue());
                    for(RippleData value: data)
                    {
                        // TODO: convert ADC value?
                        valueEntry.setValue(""+((ECGData)value).adcReading);
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITALS, dataCols);
                    }
                    break;
                case SENSOR_TEMPERATURE:
                    valueTypeEntry.setValue(""+VITAL_TYPES.VITAL_TEMPERATURE.getValue());
                    for(RippleData value: data)
                    {
                        valueEntry.setValue(""+((TemperatureData)value).temperature);
                        this.databaseHelper.insertRow(Reference.TABLE_NAMES.VITALS, dataCols);
                    }
                    break;
                default:
                    throw new AssertionError(msg.getSensorType().name());
            }
        }
    }
    
}
