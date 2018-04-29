package com.github.connteam.conn.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultiEventListener<T> implements EventListener<T> {
    private Map<Class<?>, List<Method>> handlers;

    public static class EventHandlerException extends RuntimeException {
        private static final long serialVersionUID = 537059402186303028L;
        
        public EventHandlerException() {}

        public EventHandlerException(String msg) {
            super(msg);
        }
        
        public EventHandlerException(Throwable err) {
            super(err);
        }
    }

    public MultiEventListener() {
        handlers = new HashMap<>();

        for (Method method : getClass().getMethods()) {
            scanMethod(method);
        }
    }

    private void scanMethod(Method method) {
        if (!method.isAnnotationPresent(HandleEvent.class)) {
            return;
        }

        if (!method.getReturnType().equals(Void.TYPE)) {
            throw new EventHandlerException("Event handler must return void");
        }
        if (method.getParameterCount() != 1) {
            throw new EventHandlerException("Event handler must have only 1 argument");
        }

        Class<?> type = method.getParameters()[0].getType();

        if (!handlers.containsKey(type)) {
            handlers.put(type, new ArrayList<>());
        }

        handlers.get(type).add(method);
    }

    @Override
    public void handle(T event) {
        if (event == null) {
            return;
        }

        Class<?> type = event.getClass();

        while (type != null) {
            List<Method> list = handlers.get(type);

            if (list != null) {
                for (Method handler : list) {
                    try {
						handler.invoke(this, event);
					} catch (IllegalAccessException e) {
                        throw new EventHandlerException("Inaccesible handler");
					} catch (InvocationTargetException e) {
                        throw new EventHandlerException(e.getTargetException());
					}
                }
            }

            type = type.getSuperclass();
        }
    }
}
