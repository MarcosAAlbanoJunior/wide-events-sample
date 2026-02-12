package com.demo.wideevents.domain;

public final class WideEventContext {

    private static final ThreadLocal<WideEvent.WideEventBuilder> CONTEXT = new ThreadLocal<>();

    private WideEventContext() {}

    public static void init(WideEvent.WideEventBuilder builder) {
        CONTEXT.set(builder);
    }

    public static WideEvent.WideEventBuilder current() {
        var builder = CONTEXT.get();
        if (builder == null) {
            throw new IllegalStateException("WideEvent not initialized for this request");
        }
        return builder;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
