package mil.afrl.discoverylab.sate13.ripplebroker.data.model;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;

/**
 *
 * @author Matt
 */
public abstract class Model {

    abstract List<Map.Entry<Reference.TableColumns, String>> toListEntries();
    
    abstract void fromListEntries(List<Map.Entry<Reference.TableColumns, String>> entries);

    protected static boolean addEntry(List<Map.Entry<Reference.TableColumns, String>> entries,
                                      final Reference.TableColumns col,
                                      final String val) {
        return entries.add(new AbstractMap.SimpleEntry<Reference.TableColumns, String>(col,
                                                                                       val));
    }
}
