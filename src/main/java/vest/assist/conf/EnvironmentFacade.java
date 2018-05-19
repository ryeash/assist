package vest.assist.conf;

public class EnvironmentFacade extends ConfigurationFacadeWrapper {

    public static final String ENVIRONMENT = "environment";

    private String environment;

    public EnvironmentFacade(ConfigurationFacade delegate) {
        super(delegate);
        this.environment = delegate.get(ENVIRONMENT);
    }

    @Override
    public String get(String propertyName) {
        if (environment != null && !environment.isEmpty()) {
            String environmentValue = delegate.get(environment + '.' + propertyName);
            if (environmentValue != null) {
                return environmentValue;
            }
        }
        return delegate.get(propertyName);
    }

    @Override
    public void reload() {
        super.reload();
        this.environment = delegate.get(ENVIRONMENT);
    }
}
