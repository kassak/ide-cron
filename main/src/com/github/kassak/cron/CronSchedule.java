package com.github.kassak.cron;

import com.github.kassak.cron.schedules.UnknownSchedule;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.ScheduledFuture;

public interface CronSchedule {
  ExtensionPointName<CronSchedule> EP_NAME = ExtensionPointName.create("cron.schedule");

  @NotNull
  String getId();

  @Nullable
  ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r);

  @NotNull
  String getDescription(@Nullable Project project);

  @NotNull
  EditorDesc getEditor();

  void serialize(@NotNull Element schedule);

  CronSchedule deserialize(@NotNull Element schedule);

  class EditorDesc {
    public final JComponent component;
    public final Getter<CronSchedule> getter;

    public EditorDesc(@NotNull JComponent component, @NotNull Getter<CronSchedule> getter) {
      this.component = component;
      this.getter = getter;
    }
  }

  @NotNull
  static CronSchedule getById(@Nullable String id) {
    if (id == null) return new UnknownSchedule("unknown", null);
    CronSchedule existing = EP_NAME.getByKey(id, CronSchedule.class, CronSchedule::getId);
    return existing == null ? new UnknownSchedule(id, null) : existing;
  }
}
