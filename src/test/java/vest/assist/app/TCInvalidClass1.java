package vest.assist.app;

// validates that an injectable class must have either
// a no-arg constructor or ONE @Inject constructor
public class TCInvalidClass1 {

    public TCInvalidClass1(String param) {
    }
}
