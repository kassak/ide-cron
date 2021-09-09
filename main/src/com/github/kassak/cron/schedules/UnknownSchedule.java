package com.github.kassak.cron.schedules;

import com.github.kassak.cron.CronDaemon;
import com.github.kassak.cron.CronSchedule;
import com.github.kassak.cron.actions.UnknownAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;

import static com.github.kassak.cron.actions.UnknownAction.copyTo;

public class UnknownSchedule implements CronSchedule {
  @NotNull
  private final String myId;
  @Nullable
  private final Element mySerialized;

  public UnknownSchedule(@NotNull String id, @Nullable Element serialized) {
    myId = id;
    mySerialized = serialized;
  }

  @NotNull
  @Override
  public String getId() {
    return myId;
  }

  @Nullable
  @Override
  public ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r) {
    return null;
  }

  @NotNull
  @Override
  public String getDescription(@Nullable Project project) {
    return "Unknown schedule " + myId;
  }

  @NotNull
  @Override
  public EditorDesc getEditor() {
    JBLabel component = new JBLabel(getDescription(null));
    component.setEnabled(false);
    return new EditorDesc(component, () -> this);
  }

  @Override
  public void serialize(@NotNull Element schedule) {
    copyTo(mySerialized, schedule);
  }

  @Override
  public CronSchedule deserialize(@NotNull Element schedule) {
    return new UnknownSchedule(myId, schedule);
  }
}
