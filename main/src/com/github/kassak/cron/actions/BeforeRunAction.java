package com.github.kassak.cron.actions;

import com.github.kassak.cron.CronAction;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.containers.JBIterable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BeforeRunAction implements CronAction {
  private final String myProviderId;
  private final Element mySerialized;
  private BeforeRunTask<?> myTask;

  public BeforeRunAction() {
    this("");
  }
  public BeforeRunAction(@NotNull BeforeRunTask<?> task) {
    myProviderId = task.getProviderId().toString();
    Element rt = new Element("task");
    rt.setAttribute("provider-id", myProviderId);
    if (task instanceof PersistentStateComponent) {
      XmlSerializer.serializeStateInto((PersistentStateComponent<?>)task, rt);
    }
    else {
      task.writeExternal(rt);
    }
    mySerialized = rt;
  }

  public BeforeRunAction(@NotNull String providerId) {
    this.myProviderId = providerId;
    this.mySerialized = null;
  }

  public BeforeRunAction(@NotNull Element serialized) {
    this.myProviderId = serialized.getAttributeValue("provider-id");
    this.mySerialized = serialized;
  }

  @NotNull
  @Override
  public String getId() {
    return "BeforeRunTask";
  }

  @NotNull
  @Override
  public Icon getIcon(@NotNull Project project) {
    BeforeRunTaskProvider<BeforeRunTask<?>> provider = getProvider(project, myProviderId);
    BeforeRunTask<?> task = provider == null ? null : getTask(project);
    Icon icon = task == null ? null : provider.getTaskIcon(task);
    if (icon == null) icon = provider == null ? null : provider.getIcon();
    return icon == null ? AllIcons.Actions.Execute : icon;
  }

  @NotNull
  @Override
  public String getText(@NotNull Project project) {
    BeforeRunTaskProvider<BeforeRunTask<?>> provider = getProvider(project, myProviderId);
    BeforeRunTask<?> task = provider == null ? null : getTask(project);
    String text = task == null ? null : provider.getDescription(task);
    if (text == null) text = provider == null ? null : provider.getName();
    return text == null ? "Before Run Task" : text;
  }

  @NotNull
  @Override
  public Iterable<CronAction> getTemplates(@NotNull Project project) {
    RunConfiguration configuration = getConfiguration(project);
    return JBIterable.from(BeforeRunTaskProvider.EP_NAME.getExtensions(project))
      .filter(p -> p.createTask(configuration) != null)
      .map(p -> new BeforeRunAction(p.getId().toString()));
  }

  @NotNull
  @Override
  public CompletionStage<CronAction> edit(@NotNull DataContext context) {
    Project project = getProject(context);
    BeforeRunTaskProvider<BeforeRunTask<?>> provider = getProvider(project, myProviderId);
    if (provider == null) {
      CompletableFuture<CronAction> res = new CompletableFuture<>();
      res.completeExceptionally(new ProcessCanceledException());
      return res;
    }
    BeforeRunTask<?> task = getTask(project);
    RunConfiguration configuration = getConfiguration(project);
    if (task == null) task = provider.createTask(configuration);
    if (task == null) {
      CompletableFuture<CronAction> res = new CompletableFuture<>();
      res.completeExceptionally(new ProcessCanceledException());
      return res;
    }
    BeforeRunTask<?> newTask = task.clone();
    Promise<Boolean> res = provider.configureTask(context, configuration, task);
    CompletableFuture<CronAction> edited = new CompletableFuture<>();
    res.onProcessed(r -> {
      if (r == null || !r) {
        edited.complete(null);
      }
      else {
        edited.complete(new BeforeRunAction(newTask));
      }
    });
    return edited;
  }

  @NotNull
  @Override
  public Element serialize() {
    return mySerialized;
  }

  @NotNull
  @Override
  public CronAction deserialize(@NotNull Element element) {
    return new BeforeRunAction(element);
  }

  public void perform(@NotNull DataContext context) {
    Project project = getProject(context);
    BeforeRunTask<?> task = getTask(project);
    if (task == null) throw new ProcessCanceledException();
    RunConfiguration configuration = getConfiguration(project);
    configuration.setBeforeRunTasks(Collections.singletonList(task));
    ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configuration)
      .contentToReuse(null)
      .runner(new FakeProgramRunner())
      .dataContext(null)
      .activeTarget().build();
    ProgramRunnerUtil.executeConfiguration(env, false, true);
  }

  @NotNull
  private static Project getProject(@NotNull DataContext context) {
    return Objects.requireNonNull(PlatformDataKeys.PROJECT.getData(context));
  }

  private BeforeRunTask<?> getTask(@NotNull Project project) {
    if (myTask == null) {
      myTask = instantiate(project);
    }
    return myTask;
  }

  private static @Nullable BeforeRunTaskProvider<BeforeRunTask<?>> getProvider(@NotNull Project project, String key) {
    return BeforeRunTaskProvider.EP_NAME.findFirstSafe(project, p -> p.getId().toString().equals(key));
  }

  @Nullable
  private BeforeRunTask<?> instantiate(@NotNull Project project) {
    if (mySerialized == null) return null;
    RunConfiguration configuration = getConfiguration(project);
    BeforeRunTaskProvider<BeforeRunTask<?>> provider = getProvider(project, myProviderId);
    BeforeRunTask<?> task = provider == null ? null : provider.createTask(configuration);
    if (task != null) {
      if (task instanceof PersistentStateComponent) {
        XmlSerializer.deserializeAndLoadState((PersistentStateComponent<?>)task, mySerialized);
      }
      else {
        task.readExternal(mySerialized);
      }
    }
    return task;
  }

  @NotNull
  private RunConfiguration getConfiguration(@NotNull Project project) {
    return new FakeRunConfiguration(project);
  }

  private static class MyProcessHandler extends ProcessHandler {
    public void stop() {
      notifyProcessTerminated(0);
    }
    @Override
    protected void destroyProcessImpl() {}

    @Override
    protected void detachProcessImpl() {}

    @Override
    public boolean detachIsDefault() { return false; }

    @Override
    public @Nullable
    OutputStream getProcessInput() { return null; }
  }


  private static class FakeRunConfigurationType extends SimpleConfigurationType {
    private static final FakeRunConfigurationType INSTANCE = new FakeRunConfigurationType();
    protected FakeRunConfigurationType() {
      super("fake", "fake", null, NotNullLazyValue.createConstantValue(AllIcons.General.Error));
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new FakeRunConfiguration(project);
    }
  }
  private static class FakeRunConfiguration extends RunConfigurationBase<FakeRunConfiguration> {

    protected FakeRunConfiguration(@NotNull Project project) {
      super(project, null, "fake");
    }

    @Override
    public @NotNull ConfigurationType getType() {
      return FakeRunConfigurationType.INSTANCE;
    }



    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
      throw new AssertionError("Impossible@");
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
      return new CommandLineState(executionEnvironment) {
        @Override
        protected @Nullable ConsoleView createConsole(@NotNull Executor executor) {
          return null;
        }

        @Override
        protected @NotNull ProcessHandler startProcess() {
          MyProcessHandler ph = new MyProcessHandler();
          ph.startNotify();
          ph.stop();
          return ph;
        }
      };
    }
  }

  private static class FakeProgramRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
      return "fake";
    }

    @Override
    public boolean canRun(@NotNull String s, @NotNull RunProfile runProfile) {
      return runProfile instanceof FakeRunConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
      RunProfileState state = environment.getState();
      if (state != null) {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, () -> {
          FileDocumentManager.getInstance().saveAllDocuments();
          try {
            state.execute(environment.getExecutor(), this);
            return Promises.resolvedPromise(null);
          }
          catch (ExecutionException e) {
            return Promises.rejectedPromise(e);
          }
        });
      }
    }
  }
}
