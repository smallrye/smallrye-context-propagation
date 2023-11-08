package io.smallrye.context.impl.wrappers;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualExecutor2 implements ContextualExecutor {
    private ThreadScope<Object> tl0;
    private Object state0;
    private ThreadScope<Object> tl1;
    private Object state1;

    @Override
    public void execute(Runnable command) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        Object moved1 = tl1.get();
        tl1.set(state1);
        try {
            command.run();
        } finally {
            tl0.set(moved0);
            tl1.set(moved1);
        }
    }

    @Override
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        switch (index) {
            case 0:
                tl0 = ThreadScope;
                state0 = value;
                break;
            case 1:
                tl1 = ThreadScope;
                state1 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }

}
