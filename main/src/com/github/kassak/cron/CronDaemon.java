package com.github.kassak.cron;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.concurrency.EdtScheduledExecutorService;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@State(name = "CronDaemon", storages = @Storage("cron.xml"))
public final class CronDaemon implements PersistentStateComponent<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance(CronDaemon.class);
  @Nullable
  private final Project myProject;
  private final CronTab myTab = new CronTab();
  private final Int2ObjectMap<ScheduledFuture<?>> myPending = new Int2ObjectOpenHashMap<>();

  public CronDaemon() {
    this(null);
  }
  public CronDaemon(@Nullable Project project) {
    myProject = project;
  }

  public void updateTask(@NotNull CronTask task) {
    myTab.update(task);
    reschedule();
  }

  @NotNull
  public List<CronTask> getTasks() {
    return myTab.tasks();
  }

  public void replaceTasks(@NotNull List<CronTask> tasks) {
    myTab.replaceTasks(tasks);
    reschedule();
  }

  @Override
  public void dispose() {
    IntSet ids = freezePending();
    cancelPending(ids);
  }

  public void reschedule() {
    IntSet ids = freezePending();
    for (CronTask task : myTab.tasks()) {
      ids.remove(task.id);
      schedule(task);
    }
    cancelPending(ids);

  }

  private void cancelPending(IntSet ids) {
    for (Integer id : ids) {
      ScheduledFuture<?> stale = myPending.get(id.intValue());
      if (stale != null) stale.cancel(false);
    }
  }

  @NotNull
  private IntSet freezePending() {
    synchronized (myPending) {
      return new IntOpenHashSet(myPending.keySet());
    }
  }

  public ScheduledFuture<?> scheduleAfter(@NotNull Runnable r, long delay, @NotNull TimeUnit unit) {
    return EdtScheduledExecutorService.getInstance().schedule(r, delay, unit);
  }

  public ScheduledFuture<?> scheduleAt(@NotNull Runnable r, @NotNull LocalDateTime time) {
    long secs = LocalDateTime.now().until(time, ChronoUnit.MILLIS);
    return scheduleAfter(r, Math.max(0, secs), TimeUnit.MILLISECONDS);
  }

  private void schedule(@NotNull CronTask task) {
    ScheduledFuture<?> handle = null;
    if (task.enabled) {
      handle = task.schedule.schedule(this, () -> perform(task));
    }
    synchronized (myPending) {
      ScheduledFuture<?> prev = myPending.put(task.id, handle);
      if (prev != null) prev.cancel(false);
    }
  }

  private void perform(@NotNull CronTask task) {
    performTaskAction(task);
    schedule(task);
  }

  private void performTaskAction(@NotNull CronTask task) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame(myProject);
    if (frame == null) return;
    DataContext projectContext = DataManager.getInstance().getDataContext(frame.getComponent());
    try {
      task.action.perform(projectContext);
    }
    catch (Throwable th) {
      LOG.error("Task " + getDescription(task) + " failed, disabling it", th);
      myTab.update(task.enabled(false));
    }
  }

  private String getDescription(@NotNull CronTask task) {
    return task.description == null ? task.action.getClass().getSimpleName() : task.description;
  }

  @Override
  public @NotNull Element getState() {
    Element schedule = new Element("schedule");
    return myTab.serialize(schedule);
  }

  @Override
  public void loadState(@NotNull Element element) {
    myTab.deserialize(element);
    reschedule();
  }

  public static class Listener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
      project.getService(CronDaemon.class);
    }
  }
}
