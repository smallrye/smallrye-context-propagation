package io.smallrye.context.cdi.context.propagation;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.weld.context.WeldAlterableContext;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.contexts.cache.RequestScopedCache;
import org.jboss.weld.manager.api.WeldManager;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CdiContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        if (!isCdiAvailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        // grab WeldManager
        WeldManager weldManager = CDI.current().select(WeldManager.class).get();

        // Firstly, we need to capture beans currently active in CDI contexts
        Map<Class<? extends Annotation>, Collection<ContextualInstance<?>>> scopeToContextualInstances = new HashMap<>();
        for (WeldAlterableContext context : weldManager.getActiveWeldAlterableContexts()) {
            scopeToContextualInstances.put(context.getScope(), context.getAllContextualInstances());
        }

        return () -> {
            // firstly we need to figure out whether given context needs activation
            // if it does, then each context requires us to:
            // 1) grab a bean representing it
            // 2) prepare storage, in our case Map, and associate it with the context
            // 3) activate the context in the snapshot and propagate instances
            // 4) deactivate the context in the controller function
            // if already active, we need to:
            // 1) capture its state
            // 2) feed it instances to propagate
            // 3) restore previous state in controller function

            boolean isContextActiveOnThisThread = areContextsAlreadyActive(weldManager, scopeToContextualInstances.keySet());
            ThreadContextController controller;
            if (!isContextActiveOnThisThread) {
                // no active contexts yet, we will create Bound versions
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

                // return ThreadContextController which deactivates scopes
                controller = () -> {
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
            } else {
                // there are already active contexts here
                // capture their contents and get references to all of them
                Map<Class<? extends Annotation>, Collection<ContextualInstance<?>>> scopeToInstancesToRestoreInTheEnd = new HashMap<>();
                Map<Class<? extends Annotation>, WeldAlterableContext> scopeToContextMap = new HashMap<>();
                for (WeldAlterableContext context : weldManager.getActiveWeldAlterableContexts()) {
                    scopeToInstancesToRestoreInTheEnd.put(context.getScope(), context.getAllContextualInstances());
                    scopeToContextMap.put(context.getScope(), context);
                }

                // we work with WeldAlterableContext because the underlying implementation might differ
                WeldAlterableContext requestContext = scopeToContextMap.get(RequestScoped.class);
                WeldAlterableContext sessionContext = scopeToContextMap.get(SessionScoped.class);
                WeldAlterableContext conversationContext = scopeToContextMap.get(ConversationScoped.class);

                // propagate all contexts that have some bean in them
                if (requestContext != null && scopeToContextualInstances.get(requestContext.getScope()) != null) {
                    requestContext.clearAndSet(scopeToContextualInstances.get(requestContext.getScope()));
                }
                if (sessionContext != null && scopeToContextualInstances.get(sessionContext.getScope()) != null) {
                    sessionContext.clearAndSet(scopeToContextualInstances.get(sessionContext.getScope()));
                }
                if (conversationContext != null && scopeToContextualInstances.get(conversationContext.getScope()) != null) {
                    conversationContext.clearAndSet(scopeToContextualInstances.get(conversationContext.getScope()));
                }
                // WORKAROUND FOR WELD-2556, clear caches
                RequestScopedCache.invalidate();

                // return ThreadContextController which reverts state to what was previously on this thread
                controller = () -> {
                    if (requestContext != null && scopeToInstancesToRestoreInTheEnd.get(requestContext.getScope()) != null) {
                        requestContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(requestContext.getScope()));
                    }
                    if (sessionContext != null && scopeToInstancesToRestoreInTheEnd.get(sessionContext.getScope()) != null) {
                        sessionContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(sessionContext.getScope()));
                    }
                    if (conversationContext != null && scopeToInstancesToRestoreInTheEnd.get(conversationContext.getScope()) != null) {
                        conversationContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(conversationContext.getScope()));
                    }
                    // WORKAROUND FOR WELD-2556, clear caches
                    RequestScopedCache.invalidate();
                };
            }

            // when all is done, return the controller
            return controller;
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        if (!isCdiAvailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        // grab WeldManager
        WeldManager weldManager = CDI.current().select(WeldManager.class).get();

        // Firstly, we capture all the scopes that have active contexts, we do not care about instances here
        Set<Class<? extends Annotation>> activeScopes = new HashSet<>();
        for (WeldAlterableContext context : weldManager.getActiveWeldAlterableContexts()) {
            activeScopes.add(context.getScope());
        }

        return () -> {
            // firstly we need to figure out whether given context needs activation
            // if it does, then each context requires us to:
            // 1) grab a bean representing it
            // 2) prepare storage, in our case Map, and associate it with the context
            // 3) activate the context in the snapshot, no need for propagation
            // 4) deactivate the context in the controller function
            // if already active, we need to:
            // 1) capture its state
            // 2) feed it empty collections as new state
            // 3) restore previous state in controller function

            boolean isContextActiveOnThisThread = areContextsAlreadyActive(weldManager, activeScopes);
            ThreadContextController controller;
            if (!isContextActiveOnThisThread) {
                // no active contexts yet, we will create Bound versions
                BoundRequestContext requestContext = weldManager.instance().select(BoundRequestContext.class, BoundLiteral.INSTANCE).get();
                BoundSessionContext sessionContext = weldManager.instance().select(BoundSessionContext.class, BoundLiteral.INSTANCE).get();
                BoundConversationContext conversationContext = weldManager.instance().select(BoundConversationContext.class, BoundLiteral.INSTANCE).get();
                Map<String, Object> requestMap = null;
                Map<String, Object> sessionMap = null;

                // activation of contexts that were previously active
                if (activeScopes.contains(requestContext.getScope())) {
                    requestMap = new HashMap<>();
                    requestContext.associate(requestMap);
                    requestContext.activate();
                }
                if (activeScopes.contains(sessionContext.getScope())) {
                    sessionMap = new HashMap<>();
                    sessionContext.associate(sessionMap);
                    sessionContext.activate();
                }
                if (activeScopes.contains(conversationContext.getScope())) {
                    // for conversation scope, we need request and session storages, only proceed when that condition is met
                    if (requestMap != null && sessionMap != null) {
                        conversationContext.associate(new MutableBoundRequest(requestMap, sessionMap));
                        conversationContext.activate();
                    }
                }

                // no need for propagation, we want the storage to be empty

                // return ThreadContextController which deactivates scopes
                controller = () -> {
                    // clean up contexts we previously activated by calling deactivate() on them
                    if (activeScopes.contains(requestContext.getScope())) {
                        requestContext.deactivate();
                    }
                    if (activeScopes.contains(sessionContext.getScope())) {
                        sessionContext.deactivate();
                    }
                    if (activeScopes.contains(conversationContext.getScope())) {
                        conversationContext.deactivate();
                    }
                };
            } else {
                // there are already active contexts here

                // capture their contents and get references to all of them
                Map<Class<? extends Annotation>, Collection<ContextualInstance<?>>> scopeToInstancesToRestoreInTheEnd = new HashMap<>();
                Map<Class<? extends Annotation>, WeldAlterableContext> scopeToContextMap = new HashMap<>();
                for (WeldAlterableContext context : weldManager.getActiveWeldAlterableContexts()) {
                    scopeToInstancesToRestoreInTheEnd.put(context.getScope(), context.getAllContextualInstances());
                    scopeToContextMap.put(context.getScope(), context);
                }

                // we work with WeldAlterableContext because the underlying implementation might differ
                WeldAlterableContext requestContext = scopeToContextMap.get(RequestScoped.class);
                WeldAlterableContext sessionContext = scopeToContextMap.get(SessionScoped.class);
                WeldAlterableContext conversationContext = scopeToContextMap.get(ConversationScoped.class);

                // clear out current storage state by passing in empty collections
                if (requestContext != null & activeScopes.contains(requestContext.getScope())) {
                    requestContext.clearAndSet(Collections.emptySet());
                }
                if (sessionContext != null && activeScopes.contains(sessionContext.getScope())) {
                    sessionContext.clearAndSet(Collections.emptySet());
                }
                if (conversationContext != null && activeScopes.contains(conversationContext.getScope())) {
                    conversationContext.clearAndSet(Collections.emptySet());
                }
                // WORKAROUND FOR WELD-2556, clear caches
                RequestScopedCache.invalidate();

                // return ThreadContextController which reverts state to what was previously on this thread
                controller = () -> {
                    if (requestContext != null && scopeToInstancesToRestoreInTheEnd.get(requestContext.getScope()) != null) {
                        requestContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(requestContext.getScope()));
                    }
                    if (sessionContext != null && scopeToInstancesToRestoreInTheEnd.get(sessionContext.getScope()) != null) {
                        sessionContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(sessionContext.getScope()));
                    }
                    if (conversationContext != null && scopeToInstancesToRestoreInTheEnd.get(conversationContext.getScope()) != null) {
                        conversationContext.clearAndSet(scopeToInstancesToRestoreInTheEnd.get(conversationContext.getScope()));
                    }
                    // WORKAROUND FOR WELD-2556, clear caches
                    RequestScopedCache.invalidate();
                };
            }

            // when all is done, return the controller
            return controller;
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }

    /**
     * Checks if CDI is available within the application by using {@code CDI.current()}.
     * If an exception is thrown, it is suppressed and false is returns, otherwise true is returned.
     *
     * @return true if CDI can be used, false otherwise
     */
    private boolean isCdiAvailable() {
        try {
            return CDI.current() != null;
        } catch (IllegalStateException e) {
            // no CDI provider found, CDI isn't available
            return false;
        }
    }

    private boolean areContextsAlreadyActive(WeldManager manager, Set<Class<? extends Annotation>> scopes) {
        // if any of them is active, then all are, we basically need to differentiate if we are on the same thread
        return scopes.stream().filter(scope -> manager.isContextActive(scope)).findAny().isPresent();
    }
}
