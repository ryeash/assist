package vest.assist.conf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

/**
 * ConfigurationSource implementation that pulls properties from a {@link Properties} instance. Loads data from a
 * URL so it can support file, network, and classpath loading. Also supports reloading the properties file.
 */
public class PropertiesSource implements ConfigurationSource {

    private Properties properties;
    private final URL propertiesUrl;

    public PropertiesSource(File file) {
        Objects.requireNonNull(file);
        if (!file.exists()) {
            throw new IllegalArgumentException("properties file does not exist: " + file);
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("properties must be initialized with a file, not a directory: " + file);
        }
        try {
            this.propertiesUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException("invalid file", e);
        }
        reload();
    }

    public PropertiesSource(URL propertiesUrl) {
        this.propertiesUrl = Objects.requireNonNull(propertiesUrl);
        reload();
    }

    @Override
    public String get(String propertyName) {
        if (properties != null) {
            return properties.getProperty(propertyName);
        }
        return null;
    }

    @Override
    public void reload() {
        try (InputStream is = propertiesUrl.openStream()) {
            Properties p = new Properties();
            p.load(is);
            this.properties = p;
        } catch (IOException e) {
            throw new UncheckedIOException("error loading properties file: " + propertiesUrl, e);
        }
    }

}
