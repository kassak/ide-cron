package com.github.kassak.cron;

import com.github.kassak.cron.actions.UnknownAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface CronAction {
  ExtensionPointName<CronAction> EP_NAME = ExtensionPointName.create("cron.action");
  @NotNull
  String getId();
  @NotNull
  Icon getIcon(@Nullable Project project);
  @NotNull
  String getText(@Nullable Project project);
  void perform(@NotNull DataContext context);

  @NotNull
  Iterable<CronAction> getTemplates(@Nullable Project project);
  @NotNull
  CompletionStage<CronAction> edit(@NotNull DataContext context);
  void serialize(@NotNull Element action);
  @NotNull
  CronAction deserialize(@NotNull Element action);

  @NotNull
  static CronAction getById(@Nullable String id) {
    if (id == null) return new UnknownAction("unknown", null);
    CronAction existing = EP_NAME.getByKey(id, CronAction.class, CronAction::getId);
    return existing == null ? new UnknownAction(id, null) : existing;
  }
}
