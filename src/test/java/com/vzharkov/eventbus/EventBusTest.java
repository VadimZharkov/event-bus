package com.vzharkov.eventbus;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class EventBusTest {

    @Test
    public void syncBusIsSubscribedAndPostedEvents() {
        final String expectedString = "Test";
        final Integer expectedInteger = 10;

        final String[] strEvents = new String[2];
        final Integer[] intEvents = new Integer[2];

        final EventBus bus = EventBus.newSyncEventBus();

        bus.subscribe(String.class, s -> strEvents[0] = s);
        bus.subscribe(String.class, s -> strEvents[1] = s);
        bus.subscribe(Integer.class, i -> intEvents[0] = i);
        bus.subscribe(Integer.class, i -> intEvents[1] = i);

        bus.post("Test");
        bus.post(10);

        bus.shutdownAndAwaitTermination();

        assertEquals(expectedString, strEvents[0]);
        assertEquals(expectedString, strEvents[1]);
        assertEquals(expectedInteger, intEvents[0]);
        assertEquals(expectedInteger, intEvents[0]);
    }

    @Test
    public void asyncBusIsSubscribedAndPostedEvents() {
        final String expectedString = "Test";
        final Integer expectedInteger = 10;

        final String[] strEvents = new String[2];
        final Integer[] intEvents = new Integer[2];

        final EventBus bus = EventBus.newAsyncEventBus();

        bus.subscribe(String.class, s -> strEvents[0] = s);
        bus.subscribe(String.class, s -> strEvents[1] = s);
        bus.subscribe(Integer.class, i -> intEvents[0] = i);
        bus.subscribe(Integer.class, i -> intEvents[1] = i);

        bus.post("Test");
        bus.post(10);

        bus.shutdownAndAwaitTermination();

        assertEquals(expectedString, strEvents[0]);
        assertEquals(expectedString, strEvents[1]);
        assertEquals(expectedInteger, intEvents[0]);
        assertEquals(expectedInteger, intEvents[0]);
    }

    @Test
    public void unsubscribed() {
        final String expectedString = "Test";
        final String[] strEvents = new String[1];

        final EventBus bus = EventBus.newSyncEventBus();
        final EventBus.Token<String> token = bus.subscribe(String.class, s -> strEvents[0] = s);
        bus.post("Test");
        bus.unsubscribe(token);
        bus.post("Test2");

        bus.shutdownAndAwaitTermination();

        assertEquals(expectedString, strEvents[0]);
    }
}