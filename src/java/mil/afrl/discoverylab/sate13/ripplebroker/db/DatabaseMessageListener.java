package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripplebroker.UDPListenerObservation;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITALS_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage;
import mil.afrl.discoverylab.sate13.ripplebroker.util.RippleMoteMessage.RippleData;
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
            if(!this.databaseHelper.patientExists(msg.getSenderAddress().getAddress()))
            {
                //TODO: insert patient into database
            }
            
            int patientId = this.databaseHelper.getPatientId(msg.getSenderAddress().getAddress());
            
            List<Entry<Reference.TableColumns, String>> dataCols = new ArrayList<Entry<Reference.TableColumns, String>>();
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
                        
                    }
                    break;
                case SENSOR_ECG:
                    break;
                case SENSOR_TEMPERATURE:
                    break;
                default:
                    throw new AssertionError(msg.getSensorType().name());
            }
        }
    }
    
}
