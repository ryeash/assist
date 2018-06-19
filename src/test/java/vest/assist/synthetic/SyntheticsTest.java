package vest.assist.synthetic;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.assist.Assist;
import vest.assist.conf.Builder;
import vest.assist.conf.ConfigurationFacade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class SyntheticsTest extends Assert {

    @Test
    @SuppressWarnings("unchecked")
    public void negative() {
        Assist assist = new Assist();
        assertThrows(() -> assist.synthesize(InputStream.class, SyntheticPropertiesAspect.class));
        assertThrows(() -> assist.synthesize(SyntheticProperties.class, (Class) null));
        assertThrows(() -> assist.synthesize(SyntheticProperties.class, new Class[]{}));
    }

    @Test
    public void proxyProperties() throws IOException {
        String testFile = Files.find(new File(".").toPath(), 3, (path, basicFileAttributes) -> path.getFileName().toString().equals("test.conf"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("error finding test config"))
                .toString();

        ConfigurationFacade conf = Builder.defaultFacade(testFile);
        Assist assist = new Assist();
        assist.setSingleton(ConfigurationFacade.class, conf);

        SyntheticProperties props = assist.synthesize(SyntheticProperties.class, SyntheticPropertiesAspect.class);
        assertEquals(props.integer(), 12);
        assertEquals(props.myString(), "value");
        System.out.println(props.integers());
    }
}
