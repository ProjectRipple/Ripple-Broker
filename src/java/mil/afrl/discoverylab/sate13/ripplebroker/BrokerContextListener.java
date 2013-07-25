package mil.afrl.discoverylab.sate13.ripplebroker;

import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseMessageListener;
import mil.afrl.discoverylab.sate13.ripplebroker.network.MulticastSendListener;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.UDPToRippleListener;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Context listener for program, called when program is loaded or unloaded.
 *
 * @author james
 */
public class BrokerContextListener implements ServletContextListener {

    // Executor service for listen server
    private ExecutorService executor;
    // listen server
    private UDPListener task;
    // logger
    private Logger log;
    private MulticastSendListener multicastTask;
    private UDPToRippleListener rippleListener;
    private DatabaseMessageListener databaseListener;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Load config options
        this.initConfig(sce.getServletContext());
        // Initalize the logger
        this.initLogger(sce.getServletContext());
        // Initalize the database helper
        this.initDatabase(sce.getServletContext());
        // Get executor
        this.executor = Executors.newFixedThreadPool(2);
        try {
            // listen on anylocal address (:: or 0:0:0:0:0:0:0:0 for IPv6)
            this.task = new UDPListener(Inet6Address.getByAddress(new byte[16]), Config.LISTEN_PORT);
            // convertor to reduce parsing of ripple messages
            this.rippleListener = new UDPToRippleListener();
            // add convertor as listener to UDP
            this.task.addObserver(this.rippleListener);
            if (Config.AUTO_DATABASE_INSERT) {
                // Enable auto database inserver if config says so
                log.info("Enabling auto database insert.");
                this.databaseListener = new DatabaseMessageListener();
                //task.addObserver(this.databaseListener);
                this.rippleListener.addObserver(this.databaseListener);
            }

            this.multicastTask = new MulticastSendListener();
            //this.task.addObserver(this.multicastTask);
            this.rippleListener.addObserver(this.multicastTask);

        } catch (UnknownHostException ex) {
            log.error("UnknownHostException", ex);
        }
        // Start listen server
        executor.submit(task);

        executor.submit(this.multicastTask);

        log.debug("Context Initialized");

    }

    /**
     * Loads config options
     *
     * @param ctx
     */
    private void initConfig(ServletContext ctx) {
        // Load logger name from config
        Config.LOGGER_NAME = ctx.getInitParameter("logger.name");
        if (Config.LOGGER_NAME == null) {
            // Load default logger name
            Config.LOGGER_NAME = "ripplebrokerlogger";
            System.out.println("Error: Logger name init parameter is null.");
            System.out.println("Falling back to default value: " + Config.LOGGER_NAME);
        }

        // Load listen port from config
        try {
            Config.LISTEN_PORT = Integer.parseInt(ctx.getInitParameter("motelisten.port"));
        } catch (NumberFormatException ne) {
            // Load default port
            System.out.println("Error: Listen port init parameter is not an integer. Parameter=" + ctx.getInitParameter("motelisten.port"));
            Config.LISTEN_PORT = 1234;
            System.out.println("Falling back to default port " + Config.LISTEN_PORT);
        }

        // Load database insert option from config, only the word true (ignoring case) will result in a boolean true value
        Config.AUTO_DATABASE_INSERT = Boolean.parseBoolean(ctx.getInitParameter("database.autoinsert"));
        
        // Load multicast group address
        Config.MCAST_ADDR = ctx.getInitParameter("mcast.group");
        
        // Load multicast interface name
        Config.MCAST_INTERFACE = ctx.getInitParameter("mcast.interface");
        
        // Load multicast port number
        try {
            Config.MCAST_PORT = Integer.parseInt(ctx.getInitParameter("mcast.port"));
        } catch (NumberFormatException ne) {
            System.out.println("Error: Multicast port init parameter is not an integer. Parameter=" + ctx.getInitParameter("motelisten.port"));
            Config.MCAST_PORT = 1222;
            System.out.println("Falling back to default port " + Config.MCAST_PORT);
        }
    }

    /**
     * initalizes logger
     *
     * @param ctx
     */
    private void initLogger(ServletContext ctx) {
        // Initialize Log4j

        // Get properties path
        String prefix = ctx.getRealPath("/");
        String separator = System.getProperty("file.separator");
        String file = "WEB-INF" + separator + "classes" + separator + "log4j.properties";
        // Need to initialize app path for log4j config file logging if log is not appearing
        //System.setProperty("appRootPath", prefix);

        if (file != null) {
            // Load property file
            PropertyConfigurator.configure(prefix + file);
            log = Logger.getLogger(Config.LOGGER_NAME);
            log.debug("Log4J Logging started for application: " + prefix + file);

        } else {
            System.out.println("Log4J Is not configured for application Application: " + prefix + file);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stop listen server
        task.stop();
        // top multicast sender
        this.multicastTask.stop();
        // Stop execution service tasks
        executor.shutdown();
        log.debug("Context destroyed");
    }

    /**
     * Initialize database
     *
     * @param servletContext
     */
    private void initDatabase(ServletContext servletContext) {
        // Initalize database helper, context required only on first call to getInstance
        DatabaseHelper db = DatabaseHelper.getInstance(servletContext);

//        String ipa = "aaaa:0000:0000:0000:0000:0000:1234:0001";
//        String ipb = "aaaa:0000:0000:0000:0000:0000:5678:0001";
//
//        insertVitals(extractVitalsFromShimmerDataFile(insertTestPatient(ipa),
//                                                      servletContext.getRealPath("/"),
//                                                      "ShimmerData5"));
//        insertTestPatient(ipb);
    }

    private void insertVitals(ArrayList<Vital> vitalsList) {
        DatabaseHelper db = DatabaseHelper.getInstance(null);
        for (Vital v : vitalsList) {
            db.insertRow(Reference.TABLE_NAMES.VITAL, v.toListEntries());
        }
    }

    private ArrayList<Vital> extractVitalsFromShimmerDataFile(int pid, final String prefix, final String filename) {
        String separator = System.getProperty("file.separator");
        String file = prefix + "WEB-INF" + separator + "classes" + separator + filename;

        ArrayList<Vital> vList = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            vList = new ArrayList<Vital>();
            int vid = 0;
            while ((line = br.readLine()) != null) {
                String[] splits = line.split(";");
                vList.add(new Vital(vid++,
                    pid,
                    new Date(),
                    (int) (Double.parseDouble(splits[3]) * 1000.0),
                    Integer.toString(Reference.SENSOR_TYPES.SENSOR_ECG.getValue()),
                    Integer.toString(Reference.VITAL_TYPES.VITAL_ECG.getValue()),
                    (int) (Double.parseDouble(splits[4]) * 10000000.0)));
            }
        } catch (IOException ex) {
            log.error("Failed to read Shimmer data from file: " + file, ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ignore) {
            }
        }
        return vList;
    }

    private int insertTestPatient(String ip) {
        int pid = -1;
        // test code
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(null);
            try {
                log.debug(db.patientExists(Inet6Address.getByName(ip)));
            } catch (UnknownHostException ex) {
                log.error("Error", ex);
            }

            InetAddress address = Inet6Address.getByName(ip);

            List<Entry<Reference.TableColumns, String>> entries = new ArrayList<Entry<Reference.TableColumns, String>>();
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.FIRST_NAME, "John"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.LAST_NAME, "Doe"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.IP_ADDR, address.getHostAddress()));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.SEX, "Male"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.DOB, Reference.datetimeFormat.format(new Date())));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.TYPE, "Unknown"));
            db.insertRow(Reference.TABLE_NAMES.PATIENT, entries);

            pid = db.getPatientId(address);
        } catch (UnknownHostException ex) {
            log.error("Unknown host", ex);
        }
        return pid;
    }
}
