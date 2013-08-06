package mil.afrl.discoverylab.sate13.ripplebroker.db;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import mil.afrl.discoverylab.sate13.ripple.data.model.MultiValueVital;
import mil.afrl.discoverylab.sate13.ripple.data.model.Patient;
import mil.afrl.discoverylab.sate13.ripple.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.VITAL_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.PATIENT_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.TableColumns;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class DatabaseHelper {

    // static reference to instance
    private static DatabaseHelper instance = null;
    // database parameters
    private String databaseHost;
    private int databasePort;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;
    private String connectionURI;
    // Database objects
    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    // logger
    private static Logger log = Logger.getLogger(Config.LOGGER_NAME);
    // lock object
    private final Object lock = new Object();
    // Buffer
    private static VitalsMapBuffer vmb = new VitalsMapBuffer();
    private static MultiValueVitalsMapBuffer mvvmb = new MultiValueVitalsMapBuffer();

    /**
     * Get current instance of database helper
     *
     * @param servletContext ServletContext for application, must be initalized
     * only on first call and may be null for all subsequent calls.
     * @return DatabaseHelper instance
     */
    public static DatabaseHelper getInstance(ServletContext servletContext) {
        if (instance == null) {
            if (servletContext != null) {
                // Create helper instance
                instance = new DatabaseHelper(servletContext);
            } else {
                // error
                throw new IllegalArgumentException("Context cannot be null on first call of getInstance().");
            }
        }
        return instance;
    }

    private DatabaseHelper(ServletContext context) {
        // get database parameters
        // TODO: trust that they exist for now
        this.databaseHost = context.getInitParameter("database.host");
        this.databasePort = Integer.parseInt(context.getInitParameter("database.port"));
        this.databaseName = context.getInitParameter("database.name");
        this.databaseUser = context.getInitParameter("database.user");
        this.databasePassword = context.getInitParameter("database.password");

        // create connection URI
        this.connectionURI = "jdbc:mysql://" + databaseHost + ":" + databasePort + "/" + databaseName + "?user=" + databaseUser + "&password=" + databasePassword;
        try {
            // load MySQL driver
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            log.error("Failed to load JDBC driver", ex);
            throw new RuntimeException("Failed to load JDBC driver.", ex);
        }

        // Make sure tables exist
        this.createTables();

//        int[] test = this.selectVitalBlobTest(2, 1, 1);
//        for (int i : test) {
//            System.out.println("Value: " + i);
//        }


    }

    /**
     * Create tables for Ripple in database
     */
    private void createTables() {
        try {
            synchronized (lock) {
                this.connection = DriverManager.getConnection(this.connectionURI);
                this.statement = this.connection.createStatement();
                this.statement.execute(Reference.PATIENT_TABLE_CREATE);
                this.statement.execute(Reference.VITAL_TABLE_CREATE);
                System.out.println(Reference.VITAL_BLOB_TABLE_CREATE);
                this.statement.execute(Reference.VITAL_BLOB_TABLE_CREATE);
            }
        } catch (SQLException ex) {
            log.error("Problem creating database tables.", ex);
        } finally {
            this.closeDatabase();
        }

    }

    /**
     * Executes a SQL query for a ResultSet
     *
     * @param query
     * @return result of query
     * @throws SQLException
     */
    public CachedRowSet executeQuery(String query) throws SQLException {
        // CachedRowSet allows returning of results after original ResultSet is closed
        CachedRowSet result = null;
        try {
            synchronized (lock) {
                this.connection = DriverManager.getConnection(this.connectionURI);
                this.statement = this.connection.createStatement();
                // Use rowset provider to remove JRE implementation dependence in code
                result = RowSetProvider.newFactory().createCachedRowSet();
                // populate row set with results
                result.populate(this.statement.executeQuery(query));
            }
        } finally {
            this.closeDatabase();
        }
        return result;
    }

    /**
     * Same as executeQuery(String) but only allows select statements and uses
     * own connection
     *
     * @param query
     * @return
     * @throws SQLException
     */
    public CachedRowSet executeSelectQuery(String query) throws SQLException {
        // CachedRowSet allows returning of results after original ResultSet is closed
        CachedRowSet result = null;
        if (!(query.trim().toUpperCase().startsWith("SELECT"))) {
            return result;
        }
        Connection lConnection = null;
        Statement lStatement = null;
        try {
            lConnection = DriverManager.getConnection(this.connectionURI);
            lStatement = lConnection.createStatement();
            // Use rowset provider to remove JRE implementation dependence in code
            result = RowSetProvider.newFactory().createCachedRowSet();
            // populate row set with results
            result.populate(lStatement.executeQuery(query));

        } finally {
            try {
                if (lStatement != null) {
                    lStatement.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }

            try {
                if (lConnection != null) {
                    lConnection.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }
        }
        return result;
    }

    /**
     * Executes a SQL query for no result
     *
     * @param query
     * @throws SQLException
     */
    public void execute(String query) throws SQLException {
        try {
            synchronized (lock) {
                this.connection = DriverManager.getConnection(this.connectionURI);
                this.statement = this.connection.createStatement();
                this.statement.execute(query);
            }
        } finally {
            this.closeDatabase();
        }
    }

    /**
     * Insert row of data into database
     *
     * @param table
     * @param values
     */
    public void insertRow(Reference.TABLE_NAMES table, List<Entry<Reference.TableColumns, String>> values) {
        // start query string
        String query = "INSERT INTO " + table.toString().toLowerCase();
        String columnsString = " ";
        String valuesString = " ";

        // create column and value strings
        for (Entry<Reference.TableColumns, String> entry : values) {
            columnsString += entry.getKey().toString().toLowerCase() + ",";
            valuesString += "'" + entry.getValue() + "'" + ",";
        }
        // combine into final query string
        query += " (" + columnsString.substring(0, columnsString.length() - 1) + ") ";
        query += " VALUES(" + valuesString.substring(0, valuesString.length() - 1) + ");";

        // execute query
        try {
            this.execute(query);
        } catch (MySQLIntegrityConstraintViolationException dupex) {
            //log.debug("Omitting duplicate entry.");
        } catch (SQLException ex) {
            log.error("Error with inserting row.\nQuery String" + query, ex);
        }
    }

    public void bulkInsert(Reference.TABLE_NAMES table, List<Reference.TableColumns> columns, List<Map<Reference.TableColumns, String>> rows) {
        // check if there is something to insert
        if (columns == null || columns.isEmpty()) {
            return;
        }
        if (rows == null || rows.isEmpty()) {
            return;
        }
        // initalize string builder
        StringBuilder builder = new StringBuilder("INSERT INTO " + table.toString().toLowerCase() + " (");
        // add column names
        for (TableColumns column : columns) {
            builder.append(column.toString().toLowerCase());
            builder.append(",");
        }
        // remove last comma
        builder.deleteCharAt(builder.length() - 1);
        // Build values string
        builder.append(" ) VALUES ");
        for (Map<TableColumns, String> row : rows) {
            // Add start (
            builder.append(" (");
            for (TableColumns column : columns) {
                if (row.containsKey(column)) {
                    // String in quotes
                    builder.append("'");
                    builder.append(row.get(column));
                    builder.append("'");
                } else {
                    // If a value is missing, try inserting NULL
                    builder.append("NULL");
                }
                builder.append(",");
            }

            // remove last comma for row of values
            builder.deleteCharAt(builder.length() - 1);
            // add end )
            builder.append("),");
        }
        builder.deleteCharAt(builder.length() - 1);
        // add ; to end query
        builder.append(";");
        // debug statement
//        log.debug("Bulk insert query: " + builder.toString());
        // execute query
        try {
            this.execute(builder.toString());
        } catch (MySQLIntegrityConstraintViolationException dupex) {
            //log.debug("Omitting duplicate entry.");
        } catch (SQLException ex) {
            log.error("Error with inserting row.\nQuery String: " + builder.toString(), ex);
        }

    }

    public void insertVitalBlob(int pid, Date serverTime, long sensorTime, int sensorType, int valueType, int period, int[] values) {

        Connection conn = null;
        PreparedStatement pStatement = null;
        try {
            // Get connection and statement
            conn = DriverManager.getConnection(this.connectionURI);
            pStatement =
                conn.prepareStatement("INSERT INTO vital_blob(pid, server_timestamp, sensor_timestamp, sensor_type, value_type, num_samples, period_ms, value) VALUES(?,?,?,?,?,?,?,?);");
            // Insert values into statement
            pStatement.setInt(1, pid);
            pStatement.setTimestamp(2, new Timestamp(serverTime.getTime()));
            pStatement.setLong(3, sensorTime);
            pStatement.setInt(4, sensorType);
            pStatement.setInt(5, valueType);
            pStatement.setInt(6, values.length);
            pStatement.setInt(7, period);
            // Convert values into a byte array input stream and set a a blob
            ByteBuffer byteBuffer = ByteBuffer.allocate(values.length * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(values);
            pStatement.setBlob(8, new ByteArrayInputStream(byteBuffer.array()), values.length * 4);
            // execute statement
            pStatement.execute();

        } catch (SQLException ex) {
            log.error(DatabaseHelper.class.getName() + ":Error inserting blob", ex);
        } finally {
            try {
                if (pStatement != null) {
                    pStatement.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }
        }
    }

    public final int[] selectVitalBlobTest(int pid, int sensorType, int valueType) {
        int[] result = null;
        Connection conn = null;
        PreparedStatement pStatement = null;
        try {
            // Get connection and statement
            conn = DriverManager.getConnection(this.connectionURI);
            pStatement =
                conn.prepareStatement("SELECT num_samples, value FROM vital_blob WHERE pid=? AND sensor_type=? AND value_type=? ;");
            // Insert values into statement
            pStatement.setInt(1, pid);
            pStatement.setInt(2, sensorType);
            pStatement.setInt(3, valueType);
            // execute statement
            ResultSet rs = pStatement.executeQuery();
            if (rs.first()) {
                byte[] bArray = rs.getBytes("value");
                IntBuffer intBuf =
                    ByteBuffer.wrap(bArray)
                    .order(ByteOrder.BIG_ENDIAN)
                    .asIntBuffer();
                result = new int[intBuf.remaining()];
                intBuf.get(result);

            }

        } catch (SQLException ex) {
            log.error(DatabaseHelper.class.getName() + ":Error inserting blob", ex);
        } finally {
            try {
                if (pStatement != null) {
                    pStatement.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }
        }
        return result;
    }

    /**
     * Check if patient exists based on IP address
     *
     * @param address Patient's IP
     * @return true if patient exists in database, false otherwise
     */
    public boolean patientExists(InetAddress address) {
        boolean result = false;
        String query = "SELECT COUNT(*) as count FROM patient WHERE ip_addr='" + address.getHostAddress() + "';";
        //log.debug("Patient exists query: " + query);
        try {
            CachedRowSet rowset = this.executeQuery(query);
            rowset.last();
            result = (rowset.getInt("count") == 0) ? false : true;
            rowset.close();
        } catch (SQLException ex) {
            log.error("Failed checking for patient in table.", ex);
        }
        return result;
    }

    public List<Patient> getAllpatients() {
        StringBuilder q = new StringBuilder("SELECT * FROM ");
        q.append(Reference.TABLE_NAMES.PATIENT.toString().toLowerCase());

        log.debug("Querying all available Patients.");

        ArrayList<Patient> pList = new ArrayList<Patient>();
        try {
            CachedRowSet rs = this.executeSelectQuery(q.toString());
            rs.first();
            while (!rs.isAfterLast()) {
                pList.add(new Patient(
                    rs.getInt(PATIENT_TABLE_COLUMNS.PID.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.IP_ADDR.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.FIRST_NAME.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.LAST_NAME.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.SSN.name()),
                    rs.getDate(PATIENT_TABLE_COLUMNS.DOB.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.SEX.name()),
                    rs.getInt(PATIENT_TABLE_COLUMNS.NBC_CONTAMINATION.name()),
                    rs.getString(PATIENT_TABLE_COLUMNS.TYPE.name())));
                rs.next();
            }
            rs.close();
        } catch (SQLException ex) {
            log.error("Failed reading patient table. ", ex);
        }
        return pList;
    }

    public List<Vital> getVitalsForPatient(Integer pid, Integer vidi, Integer rowLimit, Integer timeLimit) throws SQLException {

        /*
         * select vid, pid, server_timestamp, sensor_timestamp, sensor_type, value_type, value 
         * from vital v, 
         * (select max(sensor_timestamp) as mst from vital) m 
         * where (m.mst - v.sensor_timestamp) < 10000
         */

        // TODO: use a PreparedStatement instead of just string
        String query = "select vid, pid, server_timestamp, sensor_timestamp, sensor_type, value_type, value "
            + "from vital v, "
            + "(select max(sensor_timestamp) as mst from vital where pid = " + pid + ") m "
            + "where pid = " + pid + " AND sensor_timestamp > " + vidi + " AND "
            + "(m.mst - v.sensor_timestamp) < " + timeLimit + " "
            + "ORDER BY sensor_timestamp ASC";
        if (rowLimit > 0) {
            query += " LIMIT " + rowLimit;
        }

        log.debug("Querying vitals for Patient: " + pid + ", vidi: " + vidi
            + ", rowlimit: " + rowLimit + ", timeLimit:" + timeLimit);
        ArrayList<Vital> vList = new ArrayList<Vital>();
        CachedRowSet rs = this.executeSelectQuery(query);
        log.debug("Finished query");
        boolean hasRes = rs.first();
        while (hasRes) {
            vList.add(new Vital(
                rs.getInt(VITAL_TABLE_COLUMNS.VID.name()),
                rs.getInt(VITAL_TABLE_COLUMNS.PID.name()),
                rs.getDate(VITAL_TABLE_COLUMNS.SERVER_TIMESTAMP.name()),
                rs.getLong(VITAL_TABLE_COLUMNS.SENSOR_TIMESTAMP.name()),
                rs.getString(VITAL_TABLE_COLUMNS.SENSOR_TYPE.name()),
                rs.getString(VITAL_TABLE_COLUMNS.VALUE_TYPE.name()),
                rs.getInt(VITAL_TABLE_COLUMNS.VALUE.name())));
            hasRes = rs.next();
        }
        rs.close();
        log.debug("Finished processing result set");
        return vList;
    }

    /**
     * Get a patient's ID from database or -1 if patient is not found
     *
     * @param address
     * @return
     */
    public int getPatientId(InetAddress address) {
        int result;
        String query = "SELECT pid FROM patient WHERE ip_addr='" + address.getHostAddress() + "';";
        //log.debug("Patient Id query: " + query);
        try {
            CachedRowSet rowset = this.executeQuery(query);

            if (rowset.last()) {
                result = rowset.getInt("pid");
            } else {
                result = -1;
            }
            rowset.close();
        } catch (SQLException ex) {
            log.error("Failed checking for patient id table.", ex);
            result = -1;
        }
        return result;
    }

    /**
     * Close database connection
     */
    private void closeDatabase() {
        synchronized (lock) {
            // close resultset, statement, and connection(generally in that order)
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }

            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }

            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                // Do not allow exception to propagate
            }
        }
    }

    /**
     * Closes any existing database connections and resets instance
     */
    public void cleanUp() {
        this.closeDatabase();
        instance = null;
    }

    /**
     * Buffering Related methods and class
     */
    public boolean bufferPatient(Integer pid) {
//        return vmb.addPatient(pid);
        return mvvmb.addPatient(pid);
    }

    public boolean bufferVital(Vital v) {
        return vmb.addVital(v.clone());
    }
    
    public boolean bufferMultiValueVital(MultiValueVital mvv){
        return mvvmb.addVital(mvv.clone());
    }

    public List<Vital> getBufferedVitalsForPatient(Integer pid, Long vidi, Integer rowLimit, Integer timeLimit) {
        return vmb.getVitalsAfterTime(pid, vidi);
    }
    
    public List<MultiValueVital> getBufferedMultiValueVitalsForPatient(Integer pid, Long vidi, Integer rowLimit) {
        return mvvmb.getVitalsAfterTime(pid, vidi);
    }

    private static class MultiValueVitalsMapBuffer {

        // Comparator for vital objects
        private static final MultiValueVital.VitalComparator comparator = new MultiValueVital.VitalComparator();
        // Capacity constants
        private static final Integer ENTRY_CAPACITY = 30;
        private static final Integer ENTRY_CAPACITY_FRACTION = 20;
        // Actual buffer
        private HashMap<Integer, ArrayList<MultiValueVital>> buffer;
        // current buffered vitals
        private List<MultiValueVital> bufferedVitals = new ArrayList<MultiValueVital>(ENTRY_CAPACITY);

        public MultiValueVitalsMapBuffer() {
            // init buffer
            buffer = new HashMap<Integer, ArrayList<MultiValueVital>>();
        }

        public synchronized boolean addPatient(Integer pid) {
            if (!buffer.containsKey(pid)) {
                buffer.put(pid, new ArrayList<MultiValueVital>(ENTRY_CAPACITY));
                return true;
            } else {
                return false;
            }
        }

        private synchronized boolean addVital(MultiValueVital v) {
            ArrayList<MultiValueVital> vitals = buffer.get(v.pid);
            if (vitals.size() >= ENTRY_CAPACITY) {
                vitals.remove(0);
            }
            return vitals.add(v);
        }

        private synchronized List<MultiValueVital> getVitalsAfterTime(Integer pid, Long vidi) {
            
            ArrayList<MultiValueVital> newVitals = new ArrayList<MultiValueVital>(ENTRY_CAPACITY_FRACTION);
            // check that patient is in buffer
            if(buffer.containsKey(pid)){
                // use instance list(rather than original) to avoid concurrent modification on original list
                // the instance variable can be used because method is synchronized
                // The use of an instance variable is to prevent extra object creation(such as through cloning or copy constructor)
                bufferedVitals.addAll(buffer.get(pid));
                // sort the vitals
                Collections.sort(bufferedVitals, comparator);
                Long tf = 0L;
                // Pull all vitals matching after specified time
                for (MultiValueVital v : bufferedVitals) {
                    if (v != null && v.sensor_timestamp > vidi) {
                        newVitals.add(v);
                    }
                }
                // debugging statements
//                int numVitals = newVitals.size();
//                if (numVitals > 0) {
//                    tf = newVitals.get(numVitals - 1).sensor_timestamp;
//                }
                log.debug("Found " + newVitals.size() + " out of " + bufferedVitals.size() + " buffered vitals after time " + vidi);
//                log.debug("Found " + newVitals.size() + " out of " + bufferedVitals.size()
//                    + " buffered vitals after time " + vidi
//                    + " with a min difference of " + (tf - vidi) + ".");
                
                // Clear buffer object
                bufferedVitals.clear();
            }
            return newVitals;
        }
    }
    
    private static class VitalsMapBuffer {

        // Comparator for vital objects
        private static final Vital.VitalComparator comparator = new Vital.VitalComparator();
        // Capacity constants
        private static final Integer ENTRY_CAPACITY = 100;
        private static final Integer ENTRY_CAPACITY_FRACTION = 50;
        // Actual buffer
        private HashMap<Integer, ArrayList<Vital>> buffer;
        // current buffered vitals
        private List<Vital> bufferedVitals = new ArrayList<Vital>(ENTRY_CAPACITY);

        public VitalsMapBuffer() {
            // init buffer
            buffer = new HashMap<Integer, ArrayList<Vital>>();
        }

        public synchronized boolean addPatient(Integer pid) {
            if (!buffer.containsKey(pid)) {
                buffer.put(pid, new ArrayList<Vital>(ENTRY_CAPACITY));
                return true;
            } else {
                return false;
            }
        }

        private synchronized boolean addVital(Vital v) {
            ArrayList<Vital> vitals = buffer.get(v.pid);
            if (vitals.size() >= ENTRY_CAPACITY) {
                vitals.remove(0);
            }
            return vitals.add(v);
        }

        private synchronized List<Vital> getVitalsAfterTime(Integer pid, Long vidi) {
            
            ArrayList<Vital> newVitals = new ArrayList<Vital>(ENTRY_CAPACITY_FRACTION);
            // check that patient is in buffer
            if(buffer.containsKey(pid)){
                // use instance list(rather than original) to avoid concurrent modification on original list
                // the instance variable can be used because method is synchronized
                // The use of an instance variable is to prevent extra object creation(such as through cloning or copy constructor)
                bufferedVitals.addAll(buffer.get(pid));
                // sort the vitals
                Collections.sort(bufferedVitals, comparator);
                Long tf = 0L;
                // Pull all vitals matching after specified time
                for (Vital v : bufferedVitals) {
                    if (v != null && v.sensor_timestamp > vidi) {
                        newVitals.add(v);
                    }
                }
                // debugging statements
//                int numVitals = newVitals.size();
//                if (numVitals > 0) {
//                    tf = newVitals.get(numVitals - 1).sensor_timestamp;
//                }
                log.debug("Found " + newVitals.size() + " out of " + bufferedVitals.size() + " buffered vitals after time " + vidi);
//                log.debug("Found " + newVitals.size() + " out of " + bufferedVitals.size()
//                    + " buffered vitals after time " + vidi
//                    + " with a min difference of " + (tf - vidi) + ".");
                
                // Clear buffer object
                bufferedVitals.clear();
            }
            return newVitals;
        }
    }
}
