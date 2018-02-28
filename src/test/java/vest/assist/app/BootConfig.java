package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.assist.Args;
import vest.assist.Assist;

import javax.inject.Inject;

public class BootConfig extends AppConfig {

    private static Logger log = LoggerFactory.getLogger(BootConfig.class);

    @Inject
    public BootConfig(Assist assist) {
        super(assist);
    }

    @Inject
    public void boot(Assist assist, Args args) {
        // validation expects the arguments: BootConfig -e extra --debug --withValue=something
        log.info("I'm the boot method");
        Assert.assertNotNull(assist);
        log.info("{}", args);

        Assert.assertEquals(args.first(), "vest.assist.app.BootConfig");

        Assert.assertTrue(args.flag("e"));
        Assert.assertFalse(args.flag("f"));

        Assert.assertEquals(args.flagValue("e"), "extra");

        Assert.assertTrue(args.verboseFlag("debug"));
        Assert.assertFalse(args.verboseFlag("info"));

        Assert.assertEquals(args.verboseFlagValue("withValue"), "something");
        Assert.assertNull(args.verboseFlagValue("unknown"));
        Assert.assertNull(args.verboseFlagValue("debug"));

        Assert.assertTrue(args.contains("-e"));

    }
}
