package com.github.connteam.conn.core.events;

@FunctionalInterface
public interface EventListener<T> {
    void handle(T event);
}
