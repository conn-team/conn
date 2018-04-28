package com.github.connteam.conn.core;

import static org.junit.Assert.*;
import org.junit.Test;

public class HelloFactoryTest {
    @Test
    public void helloTest() {
        assertEquals(HelloFactory.makeHello("world"), "Hello world");
    }
}
