package mil.afrl.discoverylab.sate13.ripplebroker.data.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import static mil.afrl.discoverylab.sate13.ripplebroker.data.model.Model.addEntry;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.TableColumns;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TABLE_COLUMNS;

/**
 *
 * @author burt
 */
public final class Vital extends Model {

    public Integer vid;
    public Integer pid;
    public Date server_timestamp;
    public Long sensor_timestamp;
    public String sensor_type;
    public String value_type;
    public Integer value;

    public Vital() {
    }

    public Vital(Integer vid, Integer pid,
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

    public Vital(Integer vid, Integer pid,
                 long sensor_timestamp,
                 String sensor_type, String value_type, Integer value) {
        this.vid = vid;
        this.pid = pid;
        this.sensor_timestamp = sensor_timestamp;
        this.sensor_type = sensor_type;
        this.value_type = value_type;
        this.value = value;
    }

    public Vital(Integer pid,
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
    public Vital clone() {
        return new Vital(vid, pid, (Date) server_timestamp.clone(), sensor_timestamp, sensor_type, value_type, value);
    }

    @Override
    public List<Map.Entry<Reference.TableColumns, String>> toListEntries() {
        List<Map.Entry<Reference.TableColumns, String>> entries = new ArrayList<Map.Entry<Reference.TableColumns, String>>();
        if (vid != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.VID, Integer.toString(vid));
        }
        if (pid != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.PID, Integer.toString(pid));
        }
        if (server_timestamp != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP, Reference.datetimeFormat.format(server_timestamp));
        }
        if (sensor_timestamp != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP, Long.toString(sensor_timestamp));
        }
        if (sensor_type != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.SENSOR_TYPE, sensor_type);
        }
        if (value_type != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.VALUE_TYPE, value_type);
        }
        if (value != null) {
            addEntry(entries, VITAL_TABLE_COLUMNS.VALUE, Integer.toString(value));
        }
        return entries;
    }

    @Override
    public void fromListEntries(List<Map.Entry<Reference.TableColumns, String>> entries) {
        try {
            for (Map.Entry<TableColumns, String> entry : entries) {
                VITAL_TABLE_COLUMNS key = (VITAL_TABLE_COLUMNS) entry.getKey();
                if (key == VITAL_TABLE_COLUMNS.VID) {
                    vid = Integer.valueOf(entry.getValue());
                } else if (key == VITAL_TABLE_COLUMNS.PID) {
                    pid = Integer.valueOf(entry.getValue());
                } else if (key == VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP) {
                    server_timestamp = Reference.datetimeFormat.parse(entry.getValue());
                } else if (key == VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP) {
                    sensor_timestamp = Long.valueOf(entry.getValue());
                } else if (key == VITAL_TABLE_COLUMNS.SENSOR_TYPE) {
                    sensor_type = entry.getValue();
                } else if (key == VITAL_TABLE_COLUMNS.VALUE_TYPE) {
                    value_type = entry.getValue();
                } else if (key == VITAL_TABLE_COLUMNS.VALUE) {
                    value = Integer.valueOf(entry.getValue());
                }
            }
        } catch (ParseException ex) {
        } catch (Exception e) {
        }
    }

    public static class VitalComparator implements Comparator<Vital> {

        @Override
        public int compare(Vital v1, Vital v2) {
            return v1.sensor_timestamp.compareTo(v2.sensor_timestamp);
        }
    }
}
