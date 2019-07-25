package vest.assist.synthetics.app;

import vest.assist.annotations.Property;

import java.util.List;

public interface AppProperties {

    @Property("string.prop")
    String stringProp();

    @Property("numbers")
    List<Integer> numbers();
}
