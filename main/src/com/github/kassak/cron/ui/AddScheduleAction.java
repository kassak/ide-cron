package com.github.kassak.cron.ui;

import com.github.kassak.cron.CronAction;
import com.github.kassak.cron.CronDaemon;
import com.github.kassak.cron.CronTask;
import com.github.kassak.cron.schedules.CronStyleSchedule;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class AddScheduleAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    createActionPopup(project, ac -> {
      if (ac != null) {
        project.getService(CronDaemon.class).updateTask(new CronTask(
          -1, "do smth",
          new CronStyleSchedule(new CronStyleSchedule.CronExpr("*/10", "*", "*", "*", "*", "*")),
          ac, true
        ));
      }
    }).showInBestPositionFor(e.getDataContext());
  }

  @NotNull
  public static ListPopup createActionPopup(Project project, Consumer<CronAction> finalStep) {
    return JBPopupFactory.getInstance().createListPopup(new CronActionTypeStep(project, finalStep));
  }

  private static class CronActionTypeStep extends CronActionBaseStep {

    private final Consumer<CronAction> myFinalStep;

    public CronActionTypeStep(Project project, Consumer<CronAction> finalStep) {
      super(project, "Actions", CronAction.EP_NAME.getExtensionList());
      myFinalStep = finalStep;
    }

    @Nullable
    @Override
    public PopupStep<?> onChosen(CronAction selectedValue, boolean finalChoice) {
      if (selectedValue != null) {
        return new CronActionTemplatesStep(project, selectedValue, myFinalStep);
      }
      return super.onChosen(null, finalChoice);
    }

    @Override
    public boolean hasSubstep(CronAction selectedValue) {
      return true;
    }
  }
  private static class CronActionTemplatesStep extends CronActionBaseStep {

    private final Consumer<CronAction> myFinalStep;

    public CronActionTemplatesStep(Project project, CronAction type, Consumer<CronAction> finalStep) {
      super(project, null, ContainerUtil.collect(type.getTemplates(project).iterator()));
      myFinalStep = finalStep;
    }

    @Nullable
    @Override
    public PopupStep<?> onChosen(CronAction selectedValue, boolean finalChoice) {
      if (selectedValue != null) {
        return doFinalStep(() -> {
          selectedValue.edit(SimpleDataContext.getProjectContext(project)).thenAccept(myFinalStep);
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
