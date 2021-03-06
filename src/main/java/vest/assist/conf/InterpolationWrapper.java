package vest.assist.conf;

/**
 * A facade wrapper that adds variable interpolation support. Overrides {@link ConfigurationFacade#get(String)} in
 * order to recursively fill variables with their values.
 * <br/>
 * Example:<br/>
 * A properties file containing:<br/>
 * <code>
 * host = 10.0.0.1<br/>
 * uri = http://${host}:3451<br/>
 * </code>
 * <br/>
 * Getting properties with interpolation support enabled:<br/>
 * <code>
 * // the ${host} variable is auto filled by calling conf.get("host")
 * assert conf.get("uri") == http://10.0.0.1:3451
 * </code>
 */
public class InterpolationWrapper extends ConfigurationFacadeWrapper {

    private final String macroOpen;
    private final String macroClose;

    public InterpolationWrapper(ConfigurationFacade delegate, String macroOpen, String macroClose) {
        super(delegate);
        this.macroOpen = macroOpen;
        this.macroClose = macroClose;
    }

    @Override
    public String get(String propertyName) {
        return fillMacro(super.get(propertyName));
    }

    private String fillMacro(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        int prev = 0;
        int i;
        while ((i = value.indexOf(macroOpen, prev)) >= 0) {
            sb.append(value, prev, i);
            i += macroOpen.length();
            prev = i;
            i = value.indexOf(macroClose, i);
            String subName = value.substring(prev, i);
            String subValue = String.valueOf(get(subName));
            sb.append(subValue);
            i += macroClose.length();
            prev = i;
        }
        sb.append(value, prev, value.length());
        return sb.toString();
    }
}
