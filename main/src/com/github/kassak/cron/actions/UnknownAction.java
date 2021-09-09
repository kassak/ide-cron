package com.github.kassak.cron.actions;

import com.github.kassak.cron.CronAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UnknownAction implements CronAction {
  @NotNull
  private final String myId;
  @Nullable
  private final Element mySerialized;

  public UnknownAction(@NotNull String id, @Nullable Element serialized) {
    myId = id;
    mySerialized = serialized;
  }

  @NotNull
  @Override
  public String getId() {
    return myId;
  }

  @NotNull
  @Override
  public Icon getIcon(@Nullable Project project) {
    return AllIcons.Nodes.Unknown;
  }

  @NotNull
  @Override
  public String getText(@Nullable Project project) {
    return "Unknown action " + myId;
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

  @Override
  public void serialize(@NotNull Element action) {
    copyTo(mySerialized, action);
  }

  public static void copyTo(@Nullable Element from, @NotNull Element to) {
    if (from != null) {
      for (Attribute attribute : from.getAttributes()) {
        to.setAttribute(attribute.getName(), attribute.getValue());
      }
      for (Content content : from.getContent()) {
        to.addContent(content);
      }
    }
  }

  @NotNull
  @Override
  public CronAction deserialize(@NotNull Element action) {
    return new UnknownAction(myId, action);
  }
}
