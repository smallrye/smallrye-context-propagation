package io.smallrye.context.logging;

import static org.jboss.logging.Logger.Level.ERROR;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "SRCP", length = 5)
public interface SmallRyeContextPropagationLogger extends BasicLogger {
    SmallRyeContextPropagationLogger ROOT_LOGGER = Logger.getMessageLogger(SmallRyeContextPropagationLogger.class,
            "io.smallrye.context");

    @LogMessage(level = ERROR)
    @Message(id = 1, value = "An error occurred beginning the ThreadContextSnapshot: %s")
    void errorBeginningThreadContextSnapshot(String errorMsg);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "An error occurred ending the ThreadContext: %s")
    void errorEndingContext(String errorMsg);

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "An error occurred getting the ThreadContextSnapshot: %s")
    void errorGettingSnapshot(String errorMsg);
}
