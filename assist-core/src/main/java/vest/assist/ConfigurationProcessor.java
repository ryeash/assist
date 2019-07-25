package vest.assist;

public interface ConfigurationProcessor extends Prioritized {

    void process(Object configuration, Assist assist);
}
