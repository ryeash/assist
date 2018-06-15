package vest.assist.conf;

/**
 * A facade wrapper that adds macro support. Overrides {@link ConfigurationFacade#get(String)} in order to
 * recursively fill macro values with their properties.
 * <br/>
 * Example:<br/>
 * A properties file containing:<br/>
 * <code>
 * host = 10.0.0.1<br/>
 * uri = http://${host}:3451<br/>
 * </code>
 * <br/>
 * Getting properties with macro support enabled:<br/>
 * <code>
 * // the ${host} macro is auto filled by calling conf.get("host")
 * conf.get("uri") => http://10.0.0.1:3451
 * </code>
 */
public class MacroSupportFacade extends ConfigurationFacadeWrapper {

    private String macroOpen;
    private String macroClose;

    public MacroSupportFacade(ConfigurationFacade delegate, String macroOpen, String macroClose) {
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
