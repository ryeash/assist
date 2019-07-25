package vest.assist;

import java.util.Comparator;

/**
 * Defines a class that can be prioritized relative to other classes of similar types.
 */
public interface Prioritized {

    Comparator<Prioritized> PRIORITIZED_COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    /**
     * The priority of this class relative to other similarly typed classes. Lower numbers are interpreted as
     * higher priority, e.g. priority 100 comes before priority 200.
     */
    default int priority() {
        return 1000;
    }
}
