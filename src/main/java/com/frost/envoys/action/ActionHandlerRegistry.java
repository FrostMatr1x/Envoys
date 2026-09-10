package com.frost.envoys.action;

import java.util.IdentityHashMap;
import java.util.Map;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.model.AbstractActionData;
import com.frost.envoys.action.model.EntityActionData;

public final class ActionHandlerRegistry {

    private final Map<Class<? extends EntityActionData>, NpcActionHandler<? extends EntityActionData>> handlers =
            new IdentityHashMap<>();

    public <T extends EntityActionData> void register(Class<T> type, NpcActionHandler<T> handler) {
        handlers.put(type, handler);
    }

    public void execute(EntityActionData action, ActionContext context) {
        NpcActionHandler<? extends EntityActionData> handler = findHandler(action.getClass());

        if (handler == null) {
            Envoys.LOGGER.warn(
                    "[Envoys] No action handler registered for type={} class={}",
                    action.getType(),
                    action.getClass().getSimpleName()
            );
            if (action instanceof AbstractActionData abstractAction) {
                context.advance(abstractAction.nextActionId);
            } else {
                context.advance(null);
            }
            return;
        }

        executeUnchecked(handler, action, context);
    }

    private NpcActionHandler<? extends EntityActionData> findHandler(Class<?> actionClass) {
        Class<?> current = actionClass;

        while (current != null && EntityActionData.class.isAssignableFrom(current)) {
            NpcActionHandler<? extends EntityActionData> handler = handlers.get(current);

            if (handler != null) {
                return handler;
            }

            current = current.getSuperclass();
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeUnchecked(NpcActionHandler handler, EntityActionData action,ActionContext context) {
        handler.execute(action, context);
    }
}
