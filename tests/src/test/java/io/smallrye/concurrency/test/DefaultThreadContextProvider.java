package io.smallrye.concurrency.test;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class DefaultThreadContextProvider implements ThreadContextProvider {

    private String type;

    private List<String> record;

    public DefaultThreadContextProvider(String type, List<String> record) {
        this.type = type;
        this.record = record;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return () -> {
            record.add("current before: " + type);
            return () -> {
                record.add("current after: " + type);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            record.add("default before: " + type);
            return () -> {
                record.add("default after: " + type);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return type;
    }
}
