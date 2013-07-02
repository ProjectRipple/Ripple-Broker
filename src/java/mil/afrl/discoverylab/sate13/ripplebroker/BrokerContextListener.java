package mil.afrl.discoverylab.sate13.ripplebroker;

import java.io.File;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author james
 */
public class BrokerContextListener implements ServletContextListener {

    private ExecutorService executor;
    private MoteListener task;
    private Logger log;
    
    private static final int LISTEN_PORT = 1234;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        this.initLogger(sce.getServletContext());
        
        executor = Executors.newSingleThreadExecutor();
        try {
            // listen on anylocal address (:: or 0:0:0:0:0:0:0:0)
            task = new MoteListener(Inet6Address.getByAddress(new byte[16]), LISTEN_PORT);
        } catch (UnknownHostException ex) {
            log.error("UnknownHostException", ex);
        }
        executor.submit(task);
        
        log.debug("Context Initialized");
        
    }
    
    private void initLogger(ServletContext ctx)
    {
        // Initialize Log4j
        
        String prefix = ctx.getRealPath("/");
        String separator = System.getProperty("file.separator") ;
        String file = "WEB-INF" + separator + "classes" + separator + "log4j.properties";
        // Need to initialize app path for log4j config file logging if log is not appearing
        //System.setProperty("appRootPath", prefix);
        
        if(file != null)
        {
            PropertyConfigurator.configure(prefix+file);
            log = Logger.getLogger("ripplebrokerlogger");
            log.debug("Log4J Logging started for application: " + prefix+file);
           
        } else
        {
            System.out.println("Log4J Is not configured for application Application: " + prefix+file);
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        task.stop();
        executor.shutdown();
        log.debug("Context destroyed");
    }
    
    private InetAddress getIP6Address(String interfaceName)
    {
        InetAddress result = null;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for(NetworkInterface netint : Collections.list(nets))
            {
                if(netint.getName() == null ? interfaceName == null : netint.getName().equals(interfaceName))
                {
                    for(InetAddress addr : Collections.list(netint.getInetAddresses()))
                    {
                        if(addr instanceof Inet6Address && addr.isAnyLocalAddress())
                        {
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
}
