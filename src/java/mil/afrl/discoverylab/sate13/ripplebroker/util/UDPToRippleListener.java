/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mil.afrl.discoverylab.sate13.ripplebroker.util;

import java.util.Observable;
import java.util.Observer;
import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPListenerObservation;

/**
 *
 * @author james
 */
public class UDPToRippleListener extends Observable implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof UDPListenerObservation)
        {
            RippleMoteMessage msg = RippleMoteMessage.parse((UDPListenerObservation)arg);
            this.setChanged();
            this.notifyObservers(msg);
        }
    }
    
}
