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
  private int myIdAllocator = 0;

  public synchronized List<CronTask> tasks() {
    return new ArrayList<>(myTasks.values());
  }

  public synchronized void update(@NotNull CronTask task) {
    putTask(task);
  }

  private void putTask(@NotNull CronTask task) {
    task = ensureIdentified(task);
    myTasks.put(task.id, task);
  }

  @NotNull
  private CronTask ensureIdentified(CronTask task) {
    if (task.id != -1) return task;
    return new CronTask(
      allocateId(), task.description,
      task.schedule, task.action,
      task.enabled
    );
  }

  public synchronized void replaceTasks(@NotNull List<CronTask> tasks) {
    clear();
    for (CronTask task : tasks) {
      putTask(task);
    }
  }

  private void clear() {
    myTasks.clear();
    myIdAllocator = 0;
  }

  private int allocateId() {
    while (myTasks.containsKey(myIdAllocator)) {
      ++myIdAllocator;
    }
    return myIdAllocator++;
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
    clear();
    for (Element task : element.getChildren("task")) {
      putTask(CronTask.deserialize(task));
    }
  }
}
