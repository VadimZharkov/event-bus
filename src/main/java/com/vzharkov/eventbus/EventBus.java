package com.vzharkov.eventbus;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EventBus {
    public enum Mode {
        SYNC,
        ASYNC
    }

    public static class Token<E> {
        public final Class<E> eventType;
        public final UUID id;

        public Token(Class<E> eventType, UUID id) {
            this.eventType = eventType;
            this.id = id;
        }
    }

    protected static class Subscription<E> implements Cloneable {
        protected final UUID id;
        protected final Consumer<E> subscriber;

        protected Subscription(UUID id, Consumer<E> subscriber) {
            this.id = id;
            this.subscriber = subscriber;
        }

        @Override
        protected Object clone() {
            return new Subscription<E>(this.id, this.subscriber);
        }
    }

    private final ExecutorService executor;
    private final Map<Class<?>, ArrayList<Subscription>> subscriptions;
    private final Mode mode;

    public EventBus(Mode mode) {
        this.executor = mode == Mode.SYNC ? Executors.newSingleThreadExecutor() : Executors.newCachedThreadPool();
        this.subscriptions = new HashMap<>();
        this.mode = mode;
    }

    public static EventBus newSyncEventBus() {
        return new EventBus(Mode.SYNC);
    }

    public static EventBus newAsyncEventBus() {
        return new EventBus(Mode.ASYNC);
    }

    public <E> Token<E> subscribe(final Class<E> eventType, final Consumer<E> subscriber) {
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(subscriber);
        final Token<E> token = new Token<>(eventType, UUID.randomUUID());
        final Subscription<E> subscription = new Subscription<>(token.id, subscriber);
        synchronized(subscriptions) {
            ArrayList<Subscription> eventSubscriptions = subscriptions.get(eventType);
            if (eventSubscriptions == null) {
                eventSubscriptions = new ArrayList<>();
                subscriptions.put(eventType, eventSubscriptions);
            } else {
                for (Subscription item : eventSubscriptions) {
                    if (item.subscriber.equals(subscriber)) {
                        throw new IllegalArgumentException("Subscriber " + subscriber.getClass() +
                                " already registered to event " + eventType.toString());
                    }
                }
            }
            eventSubscriptions.add(subscription);
        }

        return token;
    }

    public void unsubscribe(Token<?> token) {
        Objects.requireNonNull(token);
        synchronized(subscriptions) {
            final ArrayList<Subscription> eventSubscriptions = subscriptions.get(token.eventType);
            if (eventSubscriptions != null) {
                int size = eventSubscriptions.size();
                for (int i = 0; i < size; i++) {
                    Subscription subscription = eventSubscriptions.get(i);
                    if (subscription.id.equals(token.id)) {
                        eventSubscriptions.remove(i);
                        i--;
                        size--;
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public <E> void post(final E event) {
        Objects.requireNonNull(event);
        final ArrayList<Subscription> eventSubscriptions = new ArrayList<>();
        synchronized (subscriptions) {
            ArrayList<Subscription> source = subscriptions.get(event.getClass());
            if (source != null && source.size() > 0) {
                for (Subscription subscription : source) {
                    eventSubscriptions.add((Subscription<E>) subscription.clone());
                }
            }
        }
        if (eventSubscriptions.size() > 0) {
            executor.execute(() -> {
                eventSubscriptions.forEach(subscription -> subscription.subscriber.accept(event));
            });
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public void shutdownAndAwaitTermination() {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

