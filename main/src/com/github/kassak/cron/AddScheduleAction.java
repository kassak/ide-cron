package com.github.kassak.cron;

import com.github.kassak.cron.schedules.CronStyleSchedule;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class AddScheduleAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    JBPopupFactory.getInstance().createListPopup(new CronActionTypeStep(project)).showInBestPositionFor(e.getDataContext());
  }

  private static class CronActionTypeStep extends CronActionBaseStep {
    public CronActionTypeStep(Project project) {
      super(project, "Actions", CronAction.EP_NAME.getExtensionList());
    }

    @Nullable
    @Override
    public PopupStep<?> onChosen(CronAction selectedValue, boolean finalChoice) {
      if (selectedValue != null) {
        return new CronActionTemplatesStep(project, selectedValue);
      }
      return super.onChosen(null, finalChoice);
    }

    @Override
    public boolean hasSubstep(CronAction selectedValue) {
      return true;
    }
  }
  private static class CronActionTemplatesStep extends CronActionBaseStep {
    public CronActionTemplatesStep(Project project, CronAction type) {
      super(project, null, ContainerUtil.collect(type.getTemplates(project).iterator()));
    }

    @Nullable
    @Override
    public PopupStep<?> onChosen(CronAction selectedValue, boolean finalChoice) {
      if (selectedValue != null) {
        return doFinalStep(() -> {
          selectedValue.edit(SimpleDataContext.getProjectContext(project)).thenAccept(ac -> {
            if (ac != null) {
              project.getService(CronDaemon.class).updateTask(new CronTask(
                1, "do smth",
                new CronStyleSchedule(new CronStyleSchedule.CronExpr("*/10", "*", "*", "*", "*", "*")),
                ac, true
              ));
            }
          });
        });
      }
      return super.onChosen(selectedValue, finalChoice);
    }
  }
  private static class CronActionBaseStep extends BaseListPopupStep<CronAction> {
    protected final Project project;

    public CronActionBaseStep(Project project, String title, List<CronAction> actions) {
      super(title, actions);
      this.project = project;
    }

    @Override
    public @NotNull
    String getTextFor(CronAction value) {
      return value.getText(project);
    }

    @Override
    public Icon getIconFor(CronAction value) {
      return value.getIcon(project);
    }
  }
}
