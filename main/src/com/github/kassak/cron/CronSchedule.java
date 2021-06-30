package com.github.kassak.cron;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;

public interface CronSchedule {
    ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r);
}
