package mil.afrl.discoverylab.sate13.ripplebroker.util;

/**
 * Class to hold current value of configuration options
 * @author james
 */
public class Config {
    
    public static boolean AUTO_DATABASE_INSERT = false;
    public static int LISTEN_PORT = 1234;
    public static String LOGGER_NAME = "ripplebrokerlogger";

    public static String MCAST_ADDR = "ff02::1";
    public static int MCAST_PORT = 1222;
    public static String MCAST_INTERFACE = "wlan0";
    
}
