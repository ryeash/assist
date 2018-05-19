package vest.assist.conf;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Builder {

    public static ConfigurationFacade defaultFacade(String fileProperties) {
        return start()
                .environment()
                .system()
                .file(fileProperties)
                .enableMacros()
                .enableEnvironments()
                .finish();
    }

    public static Builder start() {
        return new Builder();
    }

    private final List<ConfigurationSource> sources = new ArrayList<>(3);
    private boolean caching = false;
    private boolean environment = false;
    private boolean macro = false;
    private String macroOpen;
    private String macroClose;

    public Builder system() {
        return add(System::getProperty);
    }

    public Builder environment() {
        return add(System::getenv);
    }

    public Builder file(String fileLocation) {
        return file(new File(fileLocation));
    }

    public Builder file(File propertiesFile) {
        return add(new PropertiesSource(propertiesFile));
    }

    public Builder classpathFile(String fileLocation) {
        URL url = ClassLoader.getSystemResource(fileLocation);
        return add(new PropertiesSource(url));
    }

    public Builder map(Map<String, String> map) {
        return add(map::get);
    }

    public Builder enableCaching() {
        this.caching = true;
        return this;
    }

    public Builder enableEnvironments() {
        this.environment = true;
        return this;
    }

    public Builder enableMacros() {
        this.macro = true;
        return enableMacros("${", "}");
    }

    public Builder enableMacros(String macroOpen, String macroClose) {
        this.macroOpen = macroOpen;
        this.macroClose = macroClose;
        return this;
    }

    public Builder add(ConfigurationSource source) {
        sources.add(source);
        return this;
    }

    public ConfigurationFacade finish() {
        ConfigurationFacade facade = new DefaultConfigurationFacade(sources);
        if (environment) {
            facade = new EnvironmentFacade(facade);
        }
        if (macro) {
            facade = new MacroSupportFacade(facade, macroOpen, macroClose);
        }
        if (caching) {
            facade = new CachingFacade(facade);
        }
        return facade;
    }
}
