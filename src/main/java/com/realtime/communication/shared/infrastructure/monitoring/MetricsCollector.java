package com.realtime.communication.shared.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Centralized metrics collector for application metrics.
 * Provides convenience methods for common metric types.
 */
@Component
public class MetricsCollector {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Increment a counter metric.
     */
    public void incrementCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        counters.computeIfAbsent(key, k ->
                Counter.builder(name)
                        .tags(tags)
                        .register(meterRegistry)
        ).increment();
    }

    /**
     * Increment a counter by a specific amount.
     */
    public void incrementCounter(String name, double amount, String... tags) {
        String key = buildKey(name, tags);
        counters.computeIfAbsent(key, k ->
                Counter.builder(name)
                        .tags(tags)
                        .register(meterRegistry)
        ).increment(amount);
    }

    /**
     * Record execution time of a task.
     */
    public <T> T recordTime(String name, Supplier<T> task, String... tags) {
        String key = buildKey(name, tags);
        Timer timer = timers.computeIfAbsent(key, k ->
                Timer.builder(name)
                        .tags(tags)
                        .register(meterRegistry)
        );
        return timer.record(task);
    }

    /**
     * Record execution time of a void task.
     */
    public void recordTime(String name, Runnable task, String... tags) {
        String key = buildKey(name, tags);
        Timer timer = timers.computeIfAbsent(key, k ->
                Timer.builder(name)
                        .tags(tags)
                        .register(meterRegistry)
        );
        timer.record(task);
    }

    /**
     * Record a gauge value (current state).
     */
    public void recordGauge(String name, Number value, String... tags) {
        meterRegistry.gauge(name, java.util.Arrays.asList(tags), value);
    }

    private String buildKey(String name, String... tags) {
        return name + ":" + String.join(":", tags);
    }
}

