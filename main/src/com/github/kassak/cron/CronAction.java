package com.github.kassak.cron;

import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

public interface CronAction {
  void perform(@NotNull DataContext context);
}
