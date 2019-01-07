package io.smallrye.concurrency.cdi.context.propagation;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;
import org.jboss.weld.context.WeldAlterableContext;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.manager.api.WeldManager;

import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CdiContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        // Verify CDI is available
        CDI<Object> cdi;
        try {
            cdi = CDI.current();
        } catch (IllegalStateException e) {
            // no CDI provider found, return null as per docs to state that propagation is not supported
            return null;
        }

        // grab WeldManager
        WeldManager weldManager = cdi.select(WeldManager.class).get();

        // Firstly, we need to capture beans currently active in CDI contexts
        Map<Class<? extends Annotation>, Collection<ContextualInstance<?>>> scopeToContextualInstances = new HashMap<>();
        for (WeldAlterableContext context : weldManager.getActiveWeldAlterableContexts()) {
            scopeToContextualInstances.put(context.getScope(), context.getAllContextualInstances());
        }

        return () -> {
            // each context requires us to:
            // 1) grab a bean representing it
            // 2) prepare storage, in our case Map, and associate it with the context
            // 3) activate the context in the snapshot
            // 4) deactivate the context in the controller function
            BoundRequestContext requestContext = weldManager.instance().select(BoundRequestContext.class, BoundLiteral.INSTANCE).get();
            BoundSessionContext sessionContext = weldManager.instance().select(BoundSessionContext.class, BoundLiteral.INSTANCE).get();
            BoundConversationContext conversationContext = weldManager.instance().select(BoundConversationContext.class, BoundLiteral.INSTANCE).get();
            Map<String, Object> requestMap = null;
            Map<String, Object> sessionMap = null;

            // activation of contexts that were previously active
            if (scopeToContextualInstances.containsKey(requestContext.getScope())) {
                requestMap = new HashMap<>();
                requestContext.associate(requestMap);
                requestContext.activate();
            }
            if (scopeToContextualInstances.containsKey(sessionContext.getScope())) {
                sessionMap = new HashMap<>();
                sessionContext.associate(sessionMap);
                sessionContext.activate();
            }
            if (scopeToContextualInstances.containsKey(conversationContext.getScope())) {
                // for conversation scope, we need request and session storages, only proceed when that condition is met
                if (requestMap != null && sessionMap != null) {
                    conversationContext.associate(new MutableBoundRequest(requestMap, sessionMap));
                    conversationContext.activate();
                }
            }

            // propagate all contexts that have some bean in them
            if (scopeToContextualInstances.get(requestContext.getScope()) != null) {
                requestContext.clearAndSet(scopeToContextualInstances.get(requestContext.getScope()));
            }
            if (scopeToContextualInstances.get(sessionContext.getScope()) != null) {
                sessionContext.clearAndSet(scopeToContextualInstances.get(sessionContext.getScope()));
            }
            if (scopeToContextualInstances.get(conversationContext.getScope()) != null) {
                conversationContext.clearAndSet(scopeToContextualInstances.get(conversationContext.getScope()));
            }

            // return ThreadContextController which should revert state to before propagation - e.g. clear CDI contexts
            ThreadContextController controller = () -> {
                // clean up contexts we previously activated by calling deactivate() on them
                if (scopeToContextualInstances.containsKey(requestContext.getScope())) {
                    requestContext.deactivate();
                }
                if (scopeToContextualInstances.containsKey(sessionContext.getScope())) {
                    sessionContext.deactivate();
                }
                if (scopeToContextualInstances.containsKey(conversationContext.getScope())) {
                    conversationContext.deactivate();
                }
            };

            // when all is done, return the controller
            return controller;
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        // Verify CDI is available
        CDI<Object> cdi;
        try {
            cdi = CDI.current();
        } catch (IllegalStateException e) {
            // no CDI provider found, return null as per docs to state that propagation is not supported
            return null;
        }

        // by default Application and Singleton scopes propagate anyway
        // Request, Session and Conversation are not active in any new thread, so no operation should be taken here
        return () -> () -> {};
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }
}
