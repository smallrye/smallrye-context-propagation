package io.smallrye.context.impl.wrappers;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualExecutor1 implements ContextualExecutor {
    private ThreadScope<Object> tl0;
    private Object state0;

    @Override
    public void execute(Runnable command) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            command.run();
        } finally {
            tl0.set(moved0);
        }
    }

    @Override
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        switch (index) {
            case 0:
                tl0 = ThreadScope;
                state0 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }

}
