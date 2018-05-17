package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.assist.Args;
import vest.assist.Assist;

import javax.inject.Inject;

public class BootConfig extends AppConfig {

    private static final Logger log = LoggerFactory.getLogger(BootConfig.class);

    @Inject
    public BootConfig(Assist assist) {
        super(assist);
    }

    @Inject
    public void boot(Assist assist, Args args) {
        // validation expects the arguments: vest.assist.app.BootConfig -e extra --debug --withValue=something
        log.info("I'm the boot method");
        Assert.assertNotNull(assist);
        log.info("{}", args);
        Assert.assertEquals(args.length(), 5);

        Assert.assertEquals(args.first(), "vest.assist.app.BootConfig");

        Assert.assertTrue(args.flag("e"));
        Assert.assertFalse(args.flag("f"));

        Assert.assertEquals(args.flagValue("e"), "extra");
        Assert.assertEquals(args.flagValue("e", "fallback"), "extra");
        Assert.assertEquals(args.flagValue("u", "fallback"), "fallback");

        Assert.assertTrue(args.verboseFlag("debug"));
        Assert.assertFalse(args.verboseFlag("info"));

        Assert.assertEquals(args.verboseFlagValue("withValue"), "something");
        Assert.assertEquals(args.verboseFlagValue("withValue", "fallback"), "something");
        Assert.assertEquals(args.verboseFlagValue("u", "fallback"), "fallback");
        Assert.assertNull(args.verboseFlagValue("unknown"));
        Assert.assertNull(args.verboseFlagValue("debug"));

        Assert.assertTrue(args.contains("-e"));

        Assert.assertEquals(args.first(), "vest.assist.app.BootConfig");
        Assert.assertEquals(args.second(), "-e");
        Assert.assertEquals(args.third(), "extra");
        Assert.assertEquals(args.fourth(), "--debug");
        Assert.assertEquals(args.fifth(), "--withValue=something");

        for (String arg : args) {
            log.info(arg);
        }

    }
}
