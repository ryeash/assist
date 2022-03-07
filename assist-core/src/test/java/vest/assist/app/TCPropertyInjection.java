package vest.assist.app;

import jakarta.inject.Inject;
import vest.assist.ConfigurationTest;
import vest.assist.annotations.Property;

import java.util.List;

public class TCPropertyInjection {

    private final String str;
    public final Boolean bool;
    public final int integer;
    public final List<Double> numbers;
    public ConfigurationTest.DemoEnum demoEnum;

    @Inject
    public TCPropertyInjection(@Property("string") String str,
                               @Property("boolean") Boolean bool,
                               @Property("integer") int integer,
                               @Property("numbers.list") List<Double> numbers) {
        this.str = str;
        this.bool = bool;
        this.integer = integer;
        this.numbers = numbers;
    }

    @Inject
    public void setProps(@Property("enum") ConfigurationTest.DemoEnum demoEnum) {
        this.demoEnum = demoEnum;
    }

    public String getStr() {
        return str;
    }
}
