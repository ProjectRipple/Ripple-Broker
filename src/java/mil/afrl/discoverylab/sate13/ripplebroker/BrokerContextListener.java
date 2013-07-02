package mil.afrl.discoverylab.sate13.ripplebroker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author james
 */
public class BrokerContextListener implements ServletContextListener {

    private ExecutorService executor;
    private Runnable task;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executor = Executors.newSingleThreadExecutor();
        //task = new MoteListener();
        //executor.submit(task);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executor.shutdown();
    }
}
