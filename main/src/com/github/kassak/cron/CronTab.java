package com.github.kassak.cron;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CronTab {
  private final Int2ObjectMap<CronTask> myTasks = new Int2ObjectOpenHashMap<>();

  public synchronized List<CronTask> tasks() {
    return new ArrayList<>(myTasks.values());
  }

  public synchronized void update(@NotNull CronTask task) {
    myTasks.put(task.id, task);
  }

  public synchronized void remove(@Nullable CronTask task) {
    if (task != null) remove(task.id);
  }

  public synchronized void remove(int id) {
    myTasks.remove(id);
  }
}
