package com.github.connteam.conn.core.events;

import static org.junit.Assert.*;

import java.io.IOException;

import com.github.connteam.conn.core.events.MultiEventListener.EventHandlerException;

import org.junit.Test;

public class MultiEventListenerTest {
    static StringBuilder log;

    static class BaseEvent {
        public String text;

        public BaseEvent(String s) {
            text = s;
        }
    }

    static class EventA extends BaseEvent {
        public EventA(String s) {
            super(s);
        }
    }

    static class EventB extends BaseEvent {
        public EventB(String s) {
            super(s);
        }
    }

    static class BrokenListener1 extends MultiEventListener<BaseEvent> {
        @HandleEvent
        public int dontReturn(EventA event) {
            fail();
            return 0;
        }
    }

    static class BrokenListener2 extends MultiEventListener<BaseEvent> {
        @HandleEvent
        public void invalidArgsCount(int a, int b) {
            fail();
        }
    }

    static class BrokenListener3 extends MultiEventListener<BaseEvent> {
        @HandleEvent
        public void invalidThrows(int a) throws IOException {
            fail();
        }
    }

    static class TestListener extends MultiEventListener<BaseEvent> {
        @HandleEvent
        public void onAnything(Object event) {
            log.append("object;");
        }

        @HandleEvent
        public void onWtf(int event) throws NullPointerException {
            fail();
        }

        @HandleEvent
        public void onBase(BaseEvent event) {
            log.append("base:" + event.text + ";");
        }

        @HandleEvent
        public void onA(EventA event) {
            log.append("A:" + event.text + ";");
        }

        @HandleEvent
        public void onB(EventB event) {
            log.append("B:" + event.text + ";");
        }
    }

    static class DefaultListener extends MultiEventListener<BaseEvent> {
        @Override
        public void onDefault(BaseEvent event) {
            log.append("default:" + event.text + ";");
        }

        @HandleEvent
        public void onA(EventA event) {
            log.append("A:" + event.text + ";");
        }
    }

    @Test
    public void test() {
        try {
            new BrokenListener1();
            fail();
        } catch (EventHandlerException ex) {
        }

        try {
            new BrokenListener2();
            fail();
        } catch (EventHandlerException ex) {
        }

        try {
            new BrokenListener3();
            fail();
        } catch (EventHandlerException ex) {
        }

        log = new StringBuilder();

        TestListener tmp = new TestListener();
        tmp.handle(new BaseEvent("1"));
        log.append(" ");
        tmp.handle(new EventA("2"));
        log.append(" ");
        tmp.handle(new EventB("3"));
        log.append(" ");

        DefaultListener def = new DefaultListener();
        def.handle(new EventA("2"));
        log.append(" ");
        def.handle(new EventB("3"));

        assertEquals("base:1;object; A:2;base:2;object; B:3;base:3;object; A:2; default:3;", log.toString());
    }
}
