package io.smallrye.context;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * Subtype of ThreadContextProvider which bypasses all the {@link #currentContext(Map)} and
 * {@link #clearedContext(Map)} and {@link ThreadContextSnapshot} and just works on the
 * ThreadLocal and cleared value we designate.
 */
public interface FastThreadContextProvider extends ThreadContextProvider {

    /**
     * Designates the ThreadLocal that we should capture/restore. Must always be
     * the same returned ThreadLocal.
     *
     * @param props properties
     * @return the ThreadLocal to capture/restore
     */
    ThreadLocal<?> threadLocal(Map<String, String> props);

    /**
     * The cleared value. Defaults to null. Override this if your cleared value
     * is not null.
     *
     * @param props properties
     * @return the cleared value for the ThreadLocal
     */
    default Object clearedValue(Map<String, String> props) {
        return null;
    }

}
