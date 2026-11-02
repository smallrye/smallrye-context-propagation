package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.context.impl.DefaultValuesFromConfig;

/**
 * The defaults read from MicroProfile Config: a key that is not defined keeps the SmallRye default, a key defined
 * with an empty value (which SmallRye Config reports as missing) means nothing, and a defined value is used as is.
 */
public class DefaultValuesFromConfigTest {
    private static final String PROPAGATED = "mp.context.ThreadContext.propagated";
    private static final String CLEARED = "mp.context.ThreadContext.cleared";

    @AfterEach
    public void cleanup() {
        System.clearProperty(PROPAGATED);
        System.clearProperty(CLEARED);
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    @Test
    public void undefinedKeysKeepTheDefaults() {
        DefaultValuesFromConfig defaults = new DefaultValuesFromConfig();
        assertArrayEquals(new String[] { ThreadContext.ALL_REMAINING }, defaults.getThreadPropagated());
        assertArrayEquals(new String[0], defaults.getThreadCleared());
    }

    @Test
    public void emptyValuesMeanNothing() {
        System.setProperty(PROPAGATED, "");
        DefaultValuesFromConfig defaults = new DefaultValuesFromConfig();
        assertArrayEquals(new String[0], defaults.getThreadPropagated());
    }

    @Test
    public void definedValuesAreUsed() {
        System.setProperty(PROPAGATED, ThreadContext.CDI + "," + ThreadContext.TRANSACTION);
        System.setProperty(CLEARED, ThreadContext.SECURITY);
        DefaultValuesFromConfig defaults = new DefaultValuesFromConfig();
        assertArrayEquals(new String[] { ThreadContext.CDI, ThreadContext.TRANSACTION }, defaults.getThreadPropagated());
        assertArrayEquals(new String[] { ThreadContext.SECURITY }, defaults.getThreadCleared());
    }
}
