package io.smallrye.context.impl;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Util {
    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }
}
