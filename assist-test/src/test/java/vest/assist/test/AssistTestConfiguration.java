package vest.assist.test;

import jakarta.inject.Singleton;
import vest.assist.annotations.Configuration;
import vest.assist.annotations.Factory;

@Configuration
public class AssistTestConfiguration {

    @Singleton
    @Factory
    public String stringFactory() {
        return "stringFactory";
    }
}
