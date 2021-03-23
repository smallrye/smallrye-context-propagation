package io.smallrye.context.impl;

import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.spi.WrappingThreadContextSnapshot;

/**
 * Restores a context and allows you to clean it up (unrestore it).
 */
public class SlowActiveContextState<T> implements CleanAutoCloseable<T> {

    private final ThreadContextController[] activeContext;
    private final CleanAutoCloseable activeThreadContext;
    private final Callable<T> callable;

    /**
     * Restores a previously captured context.
     *
     * @param threadContext the thread context
     * @param threadContextSnapshots the captured snapshots
     */
    public SlowActiveContextState(SmallRyeThreadContext threadContext, List<ThreadContextSnapshot> threadContextSnapshots,
            Callable<T> callable) {
        activeContext = new ThreadContextController[threadContextSnapshots.size()];
        int i = 0;
        for (ThreadContextSnapshot threadContextSnapshot : threadContextSnapshots) {
            activeContext[i++] = threadContextSnapshot.begin();
            if (threadContextSnapshot instanceof WrappingThreadContextSnapshot
                    && ((WrappingThreadContextSnapshot) threadContextSnapshot).needsToWrap()) {
                callable = ((WrappingThreadContextSnapshot) threadContextSnapshot).wrap(callable);
            }
        }
        this.callable = callable;
        activeThreadContext = SmallRyeThreadContext.withThreadContext(threadContext);
    }

    @Override
    public T callNoChecked() {
        try {
            return callable.call();
        } catch (Exception e) {
            Util.rethrow(e);
            return null;
        }
    }

    /**
     * Unrestores / clean-up a previously restored context.
     */
    @Override
    public void close() {
        // restore in reverse order
        for (int i = activeContext.length - 1; i >= 0; i--) {
            activeContext[i].endContext();
        }
        activeThreadContext.close();
    }
}
