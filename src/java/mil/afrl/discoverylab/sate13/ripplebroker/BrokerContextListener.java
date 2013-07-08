package mil.afrl.discoverylab.sate13.ripplebroker;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseMessageListener;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.TableColumns;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author james
 */
public class BrokerContextListener implements ServletContextListener {

    private ExecutorService executor;
    private UDPListener task;
    private Logger log;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        this.initConfig(sce.getServletContext());
        
        this.initLogger(sce.getServletContext());

        this.initDatabase(sce.getServletContext());

        executor = Executors.newSingleThreadExecutor();
        try {
            // listen on anylocal address (:: or 0:0:0:0:0:0:0:0)
            task = new UDPListener(Inet6Address.getByAddress(new byte[16]), Config.LISTEN_PORT);
            if (Config.AUTO_DATABASE_INSERT) {
                log.info("Enabling auto database insert.");
                task.addObserver(new DatabaseMessageListener());
            }
        } catch (UnknownHostException ex) {
            log.error("UnknownHostException", ex);
        }
        executor.submit(task);

        log.debug("Context Initialized");

    }
    
    private void initConfig(ServletContext ctx)
    {
        Config.LOGGER_NAME = ctx.getInitParameter("logger.name");
        if(Config.LOGGER_NAME == null){
            Config.LOGGER_NAME = "ripplebrokerlogger";
            System.out.println("Error: Logger name init parameter is null.");
            System.out.println("Falling back to default value: " + Config.LOGGER_NAME);
        }
        
        try{
            Config.LISTEN_PORT = Integer.parseInt(ctx.getInitParameter("motelisten.port"));
        } catch(NumberFormatException ne){
            System.out.println("Error: Listen port init parameter is not an integer. Parameter=" + ctx.getInitParameter("motelisten.port"));
            Config.LISTEN_PORT = 1234;
            System.out.println("Falling back to default port " + Config.LISTEN_PORT);
        }
        
        
        Config.AUTO_DATABASE_INSERT = Boolean.parseBoolean(ctx.getInitParameter("database.autoinsert"));
    }

    private void initLogger(ServletContext ctx) {
        // Initialize Log4j

        String prefix = ctx.getRealPath("/");
        String separator = System.getProperty("file.separator");
        String file = "WEB-INF" + separator + "classes" + separator + "log4j.properties";
        // Need to initialize app path for log4j config file logging if log is not appearing
        //System.setProperty("appRootPath", prefix);

        if (file != null) {
            PropertyConfigurator.configure(prefix + file);
            log = Logger.getLogger(Config.LOGGER_NAME);
            log.debug("Log4J Logging started for application: " + prefix + file);

        } else {
            System.out.println("Log4J Is not configured for application Application: " + prefix + file);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        task.stop();
        executor.shutdown();
        log.debug("Context destroyed");
    }

    private InetAddress getIP6Address(String interfaceName) {
        InetAddress result = null;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (netint.getName() == null ? interfaceName == null : netint.getName().equals(interfaceName)) {
                    for (InetAddress addr : Collections.list(netint.getInetAddresses())) {
                        if (addr instanceof Inet6Address && addr.isAnyLocalAddress()) {
                            result = addr;
                            return result;
                        }
                    }
                }

            }
        } catch (SocketException ex) {
            java.util.logging.Logger.getLogger(BrokerContextListener.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    private void initDatabase(ServletContext servletContext) {

        DatabaseHelper db = DatabaseHelper.getInstance(servletContext);

    }

    private void testDatabase() {
        // test code
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(null);
            try {
                log.debug(db.patientExists(Inet6Address.getByName("aaaa:0000:0000:0000:0000:0000:1234:0001")));
            } catch (UnknownHostException ex) {
                log.error("Error", ex);
            }

            List<Entry<Reference.TableColumns, String>> entries = new ArrayList<Entry<Reference.TableColumns, String>>();
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.FIRST_NAME, "John"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.LAST_NAME, "Doe"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.IP_ADDR, Inet6Address.getByName("aaaa:0000:0000:0000:0000:0000:1234:0001").getHostAddress()));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.SEX, "Male"));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.DOB, Reference.datetimeFormat.format(new Date())));
            entries.add(new SimpleEntry<Reference.TableColumns, String>(Reference.PATIENT_TABLE_COLUMNS.TYPE, "Unknown"));
            db.insertRow(Reference.TABLE_NAMES.PATIENT, entries);
            db.insertRow(Reference.TABLE_NAMES.PATIENT, entries);

        } catch (UnknownHostException ex) {
            log.error("Unknown host", ex);
        } 
    }
}
