package com.glance.codex.platform.paper.config.engine.reload;

import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.event.ConfigClassReloadEvent;
import com.glance.codex.platform.paper.config.engine.event.ConfigInstanceReloadEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DefaultConfigReloader implements ConfigReloader {

    private final Plugin plugin;
    private final ConcurrentMap<Class<?>, ReentrantLock> classLocks = new ConcurrentHashMap<>();

    @Inject
    public DefaultConfigReloader(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<ReloadSummary> reloadInstance(Config.@NotNull Handler instance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var result = instance.reload();
                Bukkit.getScheduler().runTask(plugin, () ->
                        new ConfigInstanceReloadEvent(instance.getClass(), instance, result).callEvent());

                return ReloadSummary.oneOk();
            } catch (Throwable t) {
                return ReloadSummary.oneFail(t.getMessage());
            }
        });
    }

    @Override
    public <T extends Config.Handler> CompletableFuture<ReloadSummary> reloadAllOf(Class<T> type) {
        final ReentrantLock lock = classLocks.computeIfAbsent(type, k -> new ReentrantLock(true));
        return CompletableFuture.supplyAsync(() -> {
            lock.lock();
            try {
                final List<T> instances = ConfigController.instancesOf(type).stream()
                        .map(type::cast).toList();

                List<CompletableFuture<Result>> futures = new ArrayList<>();
                for (T instance : instances) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        try {
                            instance.reload();
                            return Result.ok();
                        } catch (Throwable t) {
                            return Result.err(t.getMessage());
                        }
                    }));
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                int ok = 0;
                List<String> errs = new ArrayList<>();
                for (var f : futures) {
                    Result r = f.join();
                    if (r.ok) ok++; else errs.add(r.err);
                }

                ReloadSummary summary = new ReloadSummary(ok, errs);

                Bukkit.getScheduler().runTask(plugin, () ->
                        new ConfigClassReloadEvent(type, instances, summary).callEvent());

                return summary;
            } finally {
                lock.unlock();
            }
        });
    }

    private boolean isHotReloadableClass(Class<?> cls) {
        Config meta = cls.getAnnotation(Config.class);
        return meta == null || meta.supportHotReload();
    }

    @Override
    public CompletableFuture<ReloadSummary> reloadAll() {
        Map<Class<?>, List<Config.Handler>> byClass = ConfigController.allInstances()
                .stream()
                .filter(h -> isHotReloadableClass(h.getClass()))
                .collect(Collectors.groupingBy(Object::getClass));

        List<CompletableFuture<ReloadSummary>> tasks = new ArrayList<>();
        for (Class<?> cls : byClass.keySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends Config.Handler> type = (Class<? extends Config.Handler>) cls;
            tasks.add(reloadAllOf(type));
        }

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int ok = 0;
                    List<String> errs = new ArrayList<>();
                    for (var t : tasks) {
                        ReloadSummary s = t.join();
                        ok += s.ok();
                        errs.addAll(s.errors());
                    }
                    return new ReloadSummary(ok, errs);
                });
    }

    private static class Result {
        boolean ok;
        String err;

        public Result(boolean ok, String err) {
            this.ok = ok;
            this.err = err;
        }

        static Result ok() {
            return new Result(true, null);
        }
        static Result err(String err) {
            return new Result(true, err);
        }
    }

}
