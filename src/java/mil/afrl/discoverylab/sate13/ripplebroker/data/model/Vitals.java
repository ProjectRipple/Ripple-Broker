/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mil.afrl.discoverylab.sate13.ripplebroker.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITALS_TABLE_COLUMNS;

/**
 *
 * @author burt
 */
public final class Vitals extends Model {
    
    public Integer vid;
    public Integer pid;
    public Date server_timestamp;
    public long sensor_timestamp;
    public String sensor_type;
    public String value_type;
    public Integer value;

    public Vitals(Integer vid, Integer pid, 
                  Date server_timestamp, long sensor_timestamp, 
                  String sensor_type, String value_type, Integer value) {
        this.vid = vid;
        this.pid = pid;
        this.server_timestamp = server_timestamp;
        this.sensor_timestamp = sensor_timestamp;
        this.sensor_type = sensor_type;
        this.value_type = value_type;
        this.value = value;
    }

    public Vitals(Integer pid, 
                  Date server_timestamp, long sensor_timestamp, 
                  String sensor_type, String value_type, Integer value) {
        this.pid = pid;
        this.server_timestamp = server_timestamp;
        this.sensor_timestamp = sensor_timestamp;
        this.sensor_type = sensor_type;
        this.value_type = value_type;
        this.value = value;
    }

    @Override
    public List<Map.Entry<Reference.TableColumns, String>> toListEntries() {
        List<Map.Entry<Reference.TableColumns, String>> entries = new ArrayList<Map.Entry<Reference.TableColumns, String>>();

        if (vid != null) {
            addEntry(entries, VITALS_TABLE_COLUMNS.VID, Integer.toString(vid));
        }
        addEntry(entries, VITALS_TABLE_COLUMNS.PID, Integer.toString(pid));
        addEntry(entries, VITALS_TABLE_COLUMNS.SERVER_TIMESTAMP, Reference.datetimeFormat.format(server_timestamp));
        addEntry(entries, VITALS_TABLE_COLUMNS.SENSOR_TIMESTAMP, Long.toString(sensor_timestamp));
        addEntry(entries, VITALS_TABLE_COLUMNS.SENSOR_TYPE, sensor_type);
        addEntry(entries, VITALS_TABLE_COLUMNS.VALUE_TYPE, value_type);
        addEntry(entries, VITALS_TABLE_COLUMNS.VALUE, Integer.toString(value));

        return entries;
    }
    
    @Override
    void fromListEntries(List<Map.Entry<Reference.TableColumns, String>> entries) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
