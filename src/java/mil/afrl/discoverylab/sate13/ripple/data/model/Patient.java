package mil.afrl.discoverylab.sate13.ripple.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.PATIENT_TABLE_COLUMNS;

/**
 *
 * @author Matt
 */
public final class Patient extends Model {

    public Integer pid;
    public String ip_addr;
    public String first_name;
    public String last_name;
    public String ssn;
    public Date dob;
    public String sex;
    public Integer nbc_contamination;
    public String type;

    public Patient(String ip_addr) {
        this.ip_addr = ip_addr;
    }

    public Patient(Integer pid, String ip_addr, String first_name, String last_name, String ssn, Date dob, String sex, Integer nbc_contamination, String type) {
        this.pid = pid;
        this.ip_addr = ip_addr;
        this.first_name = first_name;
        this.last_name = last_name;
        this.ssn = ssn;
        this.dob = dob;
        this.sex = sex;
        this.nbc_contamination = nbc_contamination;
        this.type = type;
    }

    public Patient(String ip_addr, String first_name, String last_name, String ssn, Date dob, String sex, Integer nbc_contamination, String type) {
        this.ip_addr = ip_addr;
        this.first_name = first_name;
        this.last_name = last_name;
        this.ssn = ssn;
        this.dob = dob;
        this.sex = sex;
        this.nbc_contamination = nbc_contamination;
        this.type = type;
    }

    @Override
    public List<Map.Entry<Reference.TableColumns, String>> toListEntries() {
        List<Map.Entry<Reference.TableColumns, String>> entries = new ArrayList<Map.Entry<Reference.TableColumns, String>>();
        if (pid != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.PID, Integer.toString(pid));
        }
        if (ip_addr != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.IP_ADDR, ip_addr);
        }
        if (first_name != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.FIRST_NAME, first_name);
        }
        if (last_name != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.LAST_NAME, last_name);
        }
        if (first_name != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.SSN, ssn);
        }
        if (dob != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.DOB, Reference.datetimeFormat.format(dob));
        }
        if (sex != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.SEX, sex);
        }
        if (nbc_contamination != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.NBC_CONTAMINATION, Integer.toString(nbc_contamination));
        }
        if (type != null) {
            addEntry(entries, PATIENT_TABLE_COLUMNS.TYPE, type);
        }
        return entries;
    }

    @Override
    void fromListEntries(List<Map.Entry<Reference.TableColumns, String>> entries) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
