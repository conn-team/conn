package com.github.connteam.conn.core;

@FunctionalInterface
public interface EventListener<T> {
    void handle(T event);
}
