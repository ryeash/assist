package vest.assist.app;

import javax.inject.Inject;

// validates that an injectable class must have either
// a no-arg constructor or ONE @Inject constructor
public class TCInvalidClass2 {

    @Inject
    public TCInvalidClass2(String param) {
    }

    @Inject
    public TCInvalidClass2(String param, String param2) {
    }
}
