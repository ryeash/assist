package vest.assist.conf;

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
