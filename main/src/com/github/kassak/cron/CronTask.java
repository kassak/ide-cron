package com.github.kassak.cron;

import com.github.kassak.cron.actions.UnknownAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
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

  public String getDescription(@Nullable Project project) {
    if (description != null) return description;
    return action.getText(project);
  }

  public void serialize(Element task) {
    task.setAttribute("id", Integer.toString(id));
    if (description != null) task.setAttribute("description", description);
    if (!enabled) task.setAttribute("enabled", "false");
    Element schedule = new Element("schedule");
    schedule.setAttribute("provider-id", this.schedule.getId());
    this.schedule.serialize(schedule);
    task.addContent(schedule);
    Element action = new Element("action");
    action.setAttribute("provider-id", this.action.getId());
    this.action.serialize(action);
    task.addContent(action);
  }

  public static CronTask deserialize(Element task) {
    int id = StringUtil.parseInt(task.getAttributeValue("id"), -1);
    String description = task.getAttributeValue("description");
    boolean enabled = !"false".equals(task.getAttributeValue("enabled"));
    CronSchedule schedule = deserializeSchedule(task.getChild("schedule"));
    CronAction action = deserializeAction(task.getChild("action"));
    return new CronTask(id, description, schedule, action, enabled);
  }

  @NotNull
  private static CronAction deserializeAction(Element action) {
    String actionId = action == null ? null : action.getAttributeValue("provider-id");
    CronAction actionProvider = CronAction.getById(actionId);
    return action == null ? actionProvider : actionProvider.deserialize(action);
  }

  @NotNull
  private static CronSchedule deserializeSchedule(Element schedule) {
    String scheduleId = schedule == null ? null : schedule.getAttributeValue("provider-id");
    CronSchedule scheduleProvider = CronSchedule.getById(scheduleId);
    return schedule == null ? scheduleProvider : scheduleProvider.deserialize(schedule);
  }
}
