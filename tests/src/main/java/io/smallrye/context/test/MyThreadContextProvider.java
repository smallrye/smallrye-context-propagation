package io.smallrye.context.test;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.FastThreadContextProvider;

public class MyThreadContextProvider implements FastThreadContextProvider {

    public static final String MY_CONTEXT_TYPE = "MyContext";

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        MyContext capturedContext = MyContext.get();
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.set(capturedContext);
            return () -> {
                MyContext.set(movedContext);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.clear();
            return () -> {
                if (movedContext == null)
                    MyContext.clear();
                else
                    MyContext.set(movedContext);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return MY_CONTEXT_TYPE;
    }

    @Override
    public ThreadLocal<?> threadLocal(Map<String, String> props) {
        return MyContext.context;
    }

    @Override
    public Object clearedValue(Map<String, String> props) {
        return null;
    }

}
