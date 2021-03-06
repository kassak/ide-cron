package com.github.kassak.cron.schedules;

import com.github.kassak.cron.CronDaemon;
import com.github.kassak.cron.CronSchedule;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;

public class EmptySchedule implements CronSchedule {
  public static final EmptySchedule INSTANCE = new EmptySchedule();

  @NotNull
  @Override
  public String getId() {
      return "empty";
  }

  @Override
  public ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r) {
    return null;
  }

  @NotNull
  @Override
  public String getDescription(@Nullable Project project) {
    return "No schedule";
  }

  @NotNull
  @Override
  public EditorDesc getEditor() {
    return new EditorDesc(new JBLabel("No schedule"), () -> this);
  }

  @Override
  public void serialize(@NotNull Element schedule) {
  }

  @Override
  public CronSchedule deserialize(@NotNull Element schedule) {
    return INSTANCE;
  }
}
