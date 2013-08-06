package mil.afrl.discoverylab.sate13.ripple.data.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * @author burt
 */
public final class MultiValueVital implements Serializable {

    private static final long serialVersionUID = 512L;
    
    public Integer vid = -1;
    public Integer pid;
    public Date server_timestamp;
    public Long sensor_timestamp;
    public Integer sensor_type;
    public Integer value_type;
    public Integer period_ms;
    public int[] values;

    public MultiValueVital() {
    }

    public MultiValueVital(Integer vid, Integer pid,
                 Date server_timestamp, long sensor_timestamp,
                 Integer sensor_type, Integer value_type, Integer period_ms, int[] values) {
        this.vid = vid;
        this.pid = pid;
        this.server_timestamp = server_timestamp;
        this.sensor_timestamp = sensor_timestamp;
        this.sensor_type = sensor_type;
        this.value_type = value_type;
        this.period_ms = period_ms;
        this.values = values;
    }

    public MultiValueVital(Integer pid,
                 Date server_timestamp, long sensor_timestamp,
                 Integer sensor_type, Integer value_type, Integer period_ms, int[] values) {
        this(new Integer(-1), pid, server_timestamp, sensor_timestamp, sensor_type, value_type, period_ms, values);
    }

    @Override
    public MultiValueVital clone() {
        return new MultiValueVital(vid, pid, (Date) server_timestamp.clone(), sensor_timestamp, sensor_type, value_type, period_ms, values);
    }


    public void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public static class VitalComparator implements Comparator<MultiValueVital> {

        @Override
        public int compare(MultiValueVital v1, MultiValueVital v2) {
            return v1.sensor_timestamp.compareTo(v2.sensor_timestamp);
        }
    }
}
