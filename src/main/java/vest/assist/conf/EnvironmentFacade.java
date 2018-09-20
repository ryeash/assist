package vest.assist.conf;

/**
 * A facade wrapper that adds environment specific support for property values.
 * This facade overrides {@link ConfigurationFacade#get(String)} to prepend the
 * value of the environment property to the property name used. If an environment specific property is found
 * that value is used, otherwise falls back to using the property name as-is.
 * <br/>
 * Example:
 * <br/>
 * A properties file containing:<br/>
 * <code>
 * environment = test<br/>
 * <br/>
 * test.password = Passw0rd<br/>
 * password = dummy<br/>
 * username = administrator<br/>
 * </code>
 * <br/>
 * Getting different property values:<br/>
 * <code>
 * // tries to find 'test.password' first<br/>
 * conf.get("password") =&gt; "Passw0rd"<br/>
 * <br/>
 * // tries to find 'test.username' first<br/>
 * // then falls back to just 'username'<br/>
 * assert conf.get("username") == "administrator"
 * </code>
 */
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
