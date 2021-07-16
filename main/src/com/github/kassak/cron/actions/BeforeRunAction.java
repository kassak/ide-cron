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
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.JBIterable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.io.OutputStream;
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void perform(@NotNull DataContext context) {
    Project project = getProject(context);
    BeforeRunTask<?> task = getTask(project);
    if (task == null) throw new ProcessCanceledException();
    executeTask(project, context, (BeforeRunTask)task);
  }

  @NotNull
  private static Project getProject(@NotNull DataContext context) {
    return Objects.requireNonNull(PlatformDataKeys.PROJECT.getData(context));
  }

  private <T extends BeforeRunTask<T>> void executeTask(@NotNull Project project, @NotNull DataContext context, @NotNull T task) {
    RunConfiguration configuration = getConfiguration(project);
    Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    ExecutionEnvironment env = ExecutionEnvironmentBuilder.Companion.create(executor, configuration)
      .dataContext(context)
      .build();
    ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY.set(env, env.getExecutionId());
    BeforeRunTaskProvider<T> provider = BeforeRunTaskProvider.getProvider(project, task.getProviderId());
    if (provider == null || !provider.canExecuteTask(configuration, task)) throw new ProcessCanceledException();
    provider.executeTask(context, configuration, env, task);
    simulateWork(project, executor, env);
  }

  private void simulateWork(@NotNull Project project, Executor executor, ExecutionEnvironment env) {
    ExecutionListener publisher = project.getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC);
    MyProcessHandler ph = new MyProcessHandler();
    publisher.processStarting(executor.getId(), env, ph);
    ph.startNotify();
    publisher.processStarted(executor.getId(), env, ph);
    publisher.processTerminating(executor.getId(), env, ph);
    ph.stop();
    publisher.processTerminated(executor.getId(), env, ph, 0);
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
    return UnknownConfigurationType.getInstance().createTemplateConfiguration(project);
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
}
