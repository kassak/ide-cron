package com.github.kassak.cron.actions;

import com.github.kassak.cron.CronAction;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RunConfigurationAction implements CronAction {
  private final String myRunConfigurationName;

  public RunConfigurationAction() {
    this("");
  }
  public RunConfigurationAction(@NotNull String runConfigurationName) {
    myRunConfigurationName = runConfigurationName;
  }

  @NotNull
  @Override
  public String getId() {
    return "RunConfiguration";
  }

  @NotNull
  @Override
  public Icon getIcon(@NotNull Project project) {
    RunConfiguration runConfiguration = getRunConfiguration(project);
    Icon icon = runConfiguration == null ? null : runConfiguration.getIcon();
    return icon == null ? AllIcons.Actions.Execute : icon;
  }

  @Nullable
  private RunConfiguration getRunConfiguration(@NotNull Project project) {
    RunnerAndConfigurationSettings settings = getRunConfigurationSettings(project);
    return settings == null ? null : settings.getConfiguration();
  }

  @Nullable
  private RunnerAndConfigurationSettings getRunConfigurationSettings(@NotNull Project project) {
    return RunManager.getInstance(project).findConfigurationByName(myRunConfigurationName);
  }

  @NotNull
  @Override
  public String getText(@NotNull Project project) {
    RunConfiguration runConfiguration = getRunConfiguration(project);
    String name = runConfiguration == null ? null : runConfiguration.getName();
    return name == null ? "Run Configuration" : name;
  }

  @NotNull
  @Override
  public Iterable<CronAction> getTemplates(@NotNull Project project) {
    return JBIterable.from(RunManager.getInstance(project).getAllConfigurationsList())
      .map(c -> new RunConfigurationAction(c.getName()));
  }

  @NotNull
  @Override
  public CompletionStage<CronAction> edit(@NotNull DataContext context) {
    /*Project project = getProject(context);
    CompletableFuture<CronAction> res = new CompletableFuture<>();
    List<RunConfiguration> runConfigurations = RunManager.getInstance(project).getAllConfigurationsList();
    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<RunConfiguration>("Run Configurations", runConfigurations) {
      @Override
      public Icon getIconFor(RunConfiguration value) {
        return value.getIcon();
      }

      @Override
      public @NotNull String getTextFor(RunConfiguration value) {
        return value.getName();
      }

      @Override
      public @Nullable PopupStep<?> onChosen(RunConfiguration selectedValue, boolean finalChoice) {
        return doFinalStep(() -> {
          res.complete(new RunConfigurationAction(selectedValue.getName()));
        });
      }

      @Override
      public void setDefaultOptionIndex(int i) {
        res.complete(null);
      }
    }).showInBestPositionFor();*/
    return CompletableFuture.completedFuture(this);
  }

  @NotNull
  @Override
  public Element serialize() {
    Element res = new Element("run-configuration");
    res.setAttribute("name", myRunConfigurationName);
    return res;
  }

  @NotNull
  @Override
  public CronAction deserialize(@NotNull Element element) {
    String name = element.getAttributeValue("name", "");
    return new RunConfigurationAction(name);
  }

  @Override
  public void perform(@NotNull DataContext context) {
    Project project = getProject(context);
    RunnerAndConfigurationSettings settings = getRunConfigurationSettings(project);
    if (settings == null) throw new ProcessCanceledException();
    ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
  }

  @NotNull
  private static Project getProject(@NotNull DataContext context) {
    return Objects.requireNonNull(PlatformDataKeys.PROJECT.getData(context));
  }
}
