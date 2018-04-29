package com.github.connteam.conn.core;

import java.io.IOException;

@FunctionalInterface
public interface BinaryDecoder<T> {
    T parseFrom(byte[] data) throws IOException;
}
