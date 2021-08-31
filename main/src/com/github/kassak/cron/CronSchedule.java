package com.github.kassak.cron;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.ScheduledFuture;

public interface CronSchedule {
  @Nullable
  ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r);

  @NotNull
  String getDescription(@Nullable Project project);

  @NotNull
  EditorDesc getEditor();

  class EditorDesc {
    public final JComponent component;
    public final Getter<CronSchedule> getter;

    public EditorDesc(@NotNull JComponent component, @NotNull Getter<CronSchedule> getter) {
      this.component = component;
      this.getter = getter;
    }
  }
}
