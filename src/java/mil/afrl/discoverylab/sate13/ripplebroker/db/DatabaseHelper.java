package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
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
    private Logger log = Logger.getLogger(Reference.LOGGER_NAME);
    // lock object
    private Object lock = new Object();

    public static DatabaseHelper getInstance(ServletContext servletContext) throws ClassNotFoundException {
        if (instance == null) {
            if (servletContext != null) {
                instance = new DatabaseHelper(servletContext);
            } else {
                throw new IllegalArgumentException("Context cannot be null on first call of getInstance().");
            }
        }
        return instance;
    }

    private DatabaseHelper(ServletContext context) throws ClassNotFoundException {
        // get database parameters
        // TODO: trust that they exist for now
        this.databaseHost = context.getInitParameter("database.host");
        this.databasePort = Integer.parseInt(context.getInitParameter("database.port"));
        this.databaseName = context.getInitParameter("database.name");
        this.databaseUser = context.getInitParameter("database.user");
        this.databasePassword = context.getInitParameter("database.password");

        // create connection URI
        this.connectionURI = "jdbc:mysql://" + databaseHost + ":" + databasePort + "/" + databaseName + "?user=" + databaseUser + "&password=" + databasePassword;

        // load MySQL driver
        Class.forName("com.mysql.jdbc.Driver");

        this.createTables();

    }

    private void createTables() {
        try {
            this.connection = DriverManager.getConnection(this.connectionURI);
            this.statement = this.connection.createStatement();
            this.statement.execute(Reference.PATIENT_TABLE_CREATE);
            this.statement.execute(Reference.VITALS_TABLE_CREATE);
        } catch (SQLException ex) {
            log.error("Problem creating database tables.", ex);
        } finally {
            this.closeDatabase();
        }
    }

    private void executeQuery(String query) throws SQLException {
        try {
            this.connection = DriverManager.getConnection(this.connectionURI);
            this.statement = this.connection.createStatement();
            this.executeQuery(query);
        } finally {
            this.closeDatabase();
        }
    }
    

    /**
     * Close database connection
     */
    private void closeDatabase() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            // Do not allow exception to propagate
        }
    }

    /**
     * Closes any existing database connections and resets instance
     */
    public void cleanUp() {
        this.closeDatabase();
        instance = null;
    }
}
