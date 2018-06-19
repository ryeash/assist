package vest.assist.synthetic;

import vest.assist.annotations.Property;

import java.util.List;

public interface SyntheticProperties {

    @Property("string")
    String myString();

    @Property("integer")
    int integer();

    @Property("numbers.list")
    List<Integer> integers();

}
