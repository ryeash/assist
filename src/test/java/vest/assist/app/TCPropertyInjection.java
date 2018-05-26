package vest.assist.app;

import vest.assist.ConfigurationTest;
import vest.assist.annotations.Property;

import javax.inject.Inject;
import java.util.List;

public class TCPropertyInjection {

    @Property("string")
    private String str;

    public String getStr(){
        return str;
    }

    @Inject
    @Property("boolean")
    public Boolean bool;

    @Property("integer")
    public int integer;

    @Property("numbers.list")
    public List<Double> numbers;

    public ConfigurationTest.DemoEnum demoEnum;

    @Inject
    private void setProps(@Property("enum")ConfigurationTest.DemoEnum demoEnum){
        this.demoEnum = demoEnum;
    }
}
