package com.github.kassak.cron.actions;

import com.github.kassak.cron.CronAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class BeforeRunAction implements CronAction {
  @Override
  public void perform(@NotNull DataContext context) {
    Messages.showOkCancelDialog(context.getData(PlatformDataKeys.PROJECT), "asd", "aasd", null);
  }
}
