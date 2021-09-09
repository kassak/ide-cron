package com.github.kassak.cron;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jdom.Element;
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

  public synchronized void replaceTasks(@NotNull List<CronTask> tasks) {
    myTasks.clear();
    for (CronTask task : tasks) {
      myTasks.put(task.id, task);
    }

  }

  public synchronized void remove(@Nullable CronTask task) {
    if (task != null) remove(task.id);
  }

  public synchronized void remove(int id) {
    myTasks.remove(id);
  }

  @NotNull
  public Element serialize(@NotNull Element schedule) {
    for (CronTask task : tasks()) {
      Element t = new Element("task");
      task.serialize(t);
      schedule.addContent(t);
    }
    return schedule;
  }

  public synchronized void deserialize(@NotNull Element element) {
    myTasks.clear();
    for (Element task : element.getChildren("task")) {
      CronTask t = CronTask.deserialize(task);
      myTasks.put(t.id, t);
    }
  }
}
