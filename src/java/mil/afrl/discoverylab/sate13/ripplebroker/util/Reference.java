package mil.afrl.discoverylab.sate13.ripplebroker.util;

import java.text.SimpleDateFormat;

/**
 *
 * @author james
 */
public class Reference {

    public static final String LOGGER_NAME = "ripplebrokerlogger";
    //Sensor constants
    public static final int SENSOR_PULSE_OX = 0;
    public static final int SENSOR_ECG = 1;
    public static final int SENSOR_TEMPERATURE = 2;
    // Sensor table names and values
    public enum TABLE_NAMES{PATIENT, VITALS};
    public interface tableColumns{};
    public enum PATIENT_TABLE_COLUMNS implements tableColumns{ID, IP_ADDR, FIRST_NAME, LAST_NAME, SSN, DOB, SEX, NBC_CONTAMINATION, TYPE};
    public enum VITALS_TABLE_COLUMNS implements tableColumns{VID, PID, SERVER_TIMESTAMP, SENSOR_TIMESTAMP, SENSOR_TYPE, VALUE_TYPE, VALUE};
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final SimpleDateFormat datetimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
    // database table structures
    public static final String PATIENT_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS patient ("
        + "  id int(11) NOT NULL AUTO_INCREMENT,"
        + "  ip_addr varchar(42) NOT NULL,"
        + "  first_name varchar(20) DEFAULT NULL,"
        + "  last_name varchar(20) DEFAULT NULL,"
        + "  ssn varchar(11) DEFAULT NULL,"
        + "  dob datetime DEFAULT NULL,"
        + "  sex varchar(6) DEFAULT NULL,"
        + "  nbc_contamination int(1) DEFAULT NULL,"
        + "  type varchar(10) DEFAULT NULL,"
        + "  PRIMARY KEY (id),"
        + "  UNIQUE KEY ip_addr (ip_addr)"
        + ");";
    public static final String VITALS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS vitals ("
        + "  vid int(11) NOT NULL AUTO_INCREMENT,"
        + "  pid int(11) NOT NULL,"
        + "  server_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "  sensor_timestamp bigint(20) unsigned DEFAULT NULL,"
        + "  sensor_type varchar(10) DEFAULT NULL,"
        + "  value_type varchar(15) DEFAULT NULL,"
        + "  value int(11) DEFAULT NULL,"
        + "  PRIMARY KEY (vid,pid),"
        + "  FOREIGN KEY (pid) REFERENCES patient (id)"
        + ");";
}
