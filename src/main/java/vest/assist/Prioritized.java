package vest.assist;

import java.util.Comparator;

public interface Prioritized {

    Comparator<Prioritized> PRIORITIZED_COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    /**
     * The priority of this class relative to other similarly typed classes.
     */
    default int priority() {
        return 1000;
    }
}
