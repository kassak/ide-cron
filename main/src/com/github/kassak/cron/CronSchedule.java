package com.github.kassak.cron;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;

public interface CronSchedule {
  @Nullable
  ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r);

  @NotNull
  String getDescription(@Nullable Project project);
}
