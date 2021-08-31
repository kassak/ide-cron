package com.github.kassak.cron.actions;

import com.github.kassak.cron.CronAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EmptyAction implements CronAction {
  public static final EmptyAction INSTANCE = new EmptyAction();
  @NotNull
  @Override
  public String getId() {
    return "null";
  }

  @NotNull
  @Override
  public Icon getIcon(@Nullable Project project) {
    return AllIcons.Actions.Cancel;
  }

  @NotNull
  @Override
  public String getText(@Nullable Project project) {
    return "None";
  }

  @Override
  public void perform(@NotNull DataContext context) {

  }

  @NotNull
  @Override
  public Iterable<CronAction> getTemplates(@Nullable Project project) {
    return JBIterable.empty();
  }

  @NotNull
  @Override
  public CompletionStage<CronAction> edit(@NotNull DataContext context) {
    return CompletableFuture.completedFuture(this);
  }

  @NotNull
  @Override
  public Element serialize() {
    return new Element("");
  }

  @NotNull
  @Override
  public CronAction deserialize(@NotNull Element element) {
    return this;
  }
}
