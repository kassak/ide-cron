package com.github.kassak.cron;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CronTask {
  public final int id;
  @Nullable
  public final String description;
  public final CronSchedule schedule;
  public final CronAction action;
  public final boolean enabled;

  public CronTask(int id, @Nullable String description, @NotNull CronSchedule schedule, @NotNull CronAction action, boolean enabled) {
    this.id = id;
    this.description = description;
    this.schedule = schedule;
    this.action = action;
    this.enabled = enabled;
  }

  public CronTask enabled(boolean e) {
    return new CronTask(id, description, schedule, action, e);
  }
}
