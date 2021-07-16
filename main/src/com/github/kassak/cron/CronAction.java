package com.github.kassak.cron;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface CronAction {
  ExtensionPointName<CronAction> EP_NAME = ExtensionPointName.create("cron.action");
  @NotNull
  String getId();
  @NotNull
  Icon getIcon(@NotNull Project project);
  @NotNull
  String getText(@NotNull Project project);
  void perform(@NotNull DataContext context);

  @NotNull
  Iterable<CronAction> getTemplates(@NotNull Project project);
  @NotNull
  CompletionStage<CronAction> edit(@NotNull DataContext context);
  @NotNull
  Element serialize();
  @NotNull
  CronAction deserialize(@NotNull Element element);
}
