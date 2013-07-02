package mil.afrl.discoverylab.sate13.ripplebroker;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private Runnable task;
    private Logger log;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        // Initialize Log4j
        ServletContext ctx = sce.getServletContext();
        
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
        
        executor = Executors.newSingleThreadExecutor();
        //task = new MoteListener();
        //executor.submit(task);
        
        log.debug("Context Initialized");
        
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executor.shutdown();
        log.debug("Context destroyed");
    }
}
