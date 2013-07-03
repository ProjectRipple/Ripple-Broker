package mil.afrl.discoverylab.sate13.ripplebroker.db;

import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripplebroker.UDPListenerObservation;

/**
 * Class to automatically update database based on updates from observable listeners
 * @author james
 */
public class DatabaseMessageListener implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof UDPListenerObservation)
        {
            UDPListenerObservation obs = (UDPListenerObservation)arg;
            
        }
    }
    
}
