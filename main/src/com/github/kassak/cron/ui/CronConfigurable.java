package com.github.kassak.cron.ui;

import com.github.kassak.cron.CronAction;
import com.github.kassak.cron.CronDaemon;
import com.github.kassak.cron.CronSchedule;
import com.github.kassak.cron.CronTask;
import com.github.kassak.cron.actions.EmptyAction;
import com.github.kassak.cron.actions.UnknownAction;
import com.github.kassak.cron.schedules.CronStyleSchedule;
import com.github.kassak.cron.schedules.EmptySchedule;
import com.github.kassak.cron.schedules.UnknownSchedule;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CronConfigurable extends ConfigurableBase<CronConfigurable.CronUi, CronConfigurable.State> {
  public static final String ID = "cron.schedule";
  @Nullable
  private final Project myProject;

  public CronConfigurable() {
    this(null);
  }

  public CronConfigurable(@Nullable Project project) {
    super(ID, "Cron", null);
    myProject = project;
  }

  @Override
  protected @NotNull State getSettings() {
    State state = new State();
    state.reset(myProject);
    return state;
  }

  @Override
  protected CronUi createUi() {
    return new CronUi(myProject);
  }

  public static final class CronUi implements ConfigurableUi<State>, Disposable {
    private final Project myProject;
    private final ListTableModel<CronTask> myModel;
    private final TableView<CronTask> myTable;
    private final JComponent myComponent;

    public CronUi(@Nullable Project project) {
      myProject = project;
      myModel = new ListTableModel<CronTask>(
        new EnableColumnInfo(),
        new ScheduleColumnInfo(),
        new ActionColumnInfo(),
        new DescriptionColumnInfo()
      ) {
        @Override
        public void addRow() {
          CronStyleSchedule schedule = new CronStyleSchedule(new CronStyleSchedule.CronExpr("*", "*", "*", "*", "*", "*"));
          addRow(new CronTask(-1, null, schedule, EmptyAction.INSTANCE, true));
        }
      };
      myTable = new TableView<>(myModel);
      myComponent = ToolbarDecorator.createDecorator(myTable)
        .createPanel();
    }

    private void setTask(int idx, CronTask newTask) {
      int newIdx = myModel.getRowCount();
      myModel.addRow(newTask);
      myModel.exchangeRows(idx, newIdx);
      myModel.removeRow(newIdx);
    }

    @Override
    public void reset(@NotNull State state) {
      myModel.setItems(state.tasks);
    }

    @Override
    public boolean isModified(@NotNull State state) {
      State currentState = new State();
      save(currentState);
      return !currentState.equals(state);
    }

    @Override
    public void apply(@NotNull State state) throws ConfigurationException {
      save(state);
      state.apply(myProject);
    }

    private void save(@NotNull State state) {
      state.tasks = new ArrayList<>(myModel.getItems());
    }

    @Override
    public @NotNull JComponent getComponent() {
      return myComponent;
    }

    @Override
    public void dispose() {

    }

    private class EnableColumnInfo extends ColumnInfo<CronTask, Boolean> {
      public EnableColumnInfo() {
        super("");
      }

      @Override
      public @Nullable String getMaxStringValue() {
        return "XX";
      }

      @Override
      public @NotNull Boolean valueOf(CronTask cronTask) {
        return cronTask.enabled;
      }

      private void updateState(JTable table, boolean isSelected, boolean hasFocus, int row, int column, JComponent comp) {
        TableCellState state = new TableCellState();
        state.collectState(table, isSelected, hasFocus, row, column);
        state.updateRenderer(comp);
      }

      @Override
      public @NotNull TableCellRenderer getRenderer(CronTask cronTask) {
        class Renderer extends JCheckBox implements TableCellRenderer {

          @Override
          public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            updateState(table, isSelected, hasFocus, row, column, this);
            setSelected(Boolean.TRUE.equals(value));
            return this;
          }
        }
        return new Renderer();
      }

      @Override
      public boolean isCellEditable(CronTask cronTask) {
        return true;
      }

      @Override
      public void setValue(CronTask cronTask, Boolean value) {
        int idx = myModel.indexOf(cronTask);
        if (idx != -1) {
          CronTask newTask = new CronTask(cronTask.id, cronTask.description, cronTask.schedule, cronTask.action, Boolean.TRUE.equals(value));
          setTask(idx, newTask);
        }
      }

      @Override
      public @NotNull TableCellEditor getEditor(CronTask cronTask) {
        return new AbstractTableCellEditor() {
          private final JCheckBox myCheckBox = new JCheckBox();

          @Override
          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            updateState(table, isSelected, true, row, column, myCheckBox);
            myCheckBox.setSelected(Boolean.TRUE.equals(value));
            return myCheckBox;
          }

          @Override
          public Object getCellEditorValue() {
            return myCheckBox.isSelected();
          }
        };
      }
    }

    private class ScheduleColumnInfo extends ColumnInfo<CronTask, CronSchedule> {
      public ScheduleColumnInfo() {
        super("When");
      }

      @Override
      public @NotNull
      CronSchedule valueOf(CronTask cronTask) {
        return cronTask.schedule;
      }

      @Override
      public @NotNull TableCellRenderer getRenderer(CronTask cronTask) {
        return new ColoredTableCellRenderer() {
          @Override
          protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object v, boolean b, boolean b1, int i, int i1) {
            CronSchedule sc = ObjectUtils.tryCast(v, CronSchedule.class);
            if (sc == null) sc = EmptySchedule.INSTANCE;
            append(sc.getDescription(myProject), sc instanceof UnknownSchedule ? SimpleTextAttributes.GRAY_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
        };
      }

      @Override
      public boolean isCellEditable(CronTask cronTask) {
        return true;
      }

      @Override
      public void setValue(CronTask cronTask, CronSchedule value) {
        int idx = myModel.indexOf(cronTask);
        if (idx != -1) {
          CronTask newTask = new CronTask(cronTask.id, cronTask.description, value, cronTask.action, cronTask.enabled);
          setTask(idx, newTask);
        }
      }

      @Override
      public @NotNull TableCellEditor getEditor(CronTask cronTask) {
        return new AbstractTableCellEditor() {
          private final JComponent myPanel = new JPanel(new BorderLayout());
          private CronSchedule.EditorDesc myCurrent = EmptySchedule.INSTANCE.getEditor();
          private final ActionToolbar myToolbar;

          {
            DumbAwareAction action = new DumbAwareAction(AllIcons.Actions.MoreHorizontal) {
              @Override
              public void actionPerformed(@NotNull AnActionEvent e) {
                JBPopupFactory.getInstance().createPopupChooserBuilder(
                  ContainerUtil.filter(CronSchedule.EP_NAME.getExtensionList(), it -> !(it instanceof EmptySchedule))
                )
                  .setAutoSelectIfEmpty(false)
                  .setRenderer(new ColoredListCellRenderer<CronSchedule>() {
                    @Override
                    protected void customizeCellRenderer(@NotNull JList<? extends CronSchedule> jList, CronSchedule schedule, int i, boolean b, boolean b1) {
                      append(schedule.getDescription(myProject));
                    }
                  })
                  .setItemSelectedCallback(s -> setCurrent(s))
                  .createPopup()
                  .showInBestPositionFor(e.getDataContext());
              }
            };
            myToolbar = ActionManager.getInstance().createActionToolbar("some", new DefaultActionGroup(action), true);
//            myToolbar.setMiniMode(true);
            myToolbar.setReservePlaceAutoPopupIcon(false);
            myToolbar.setTargetComponent(myPanel);
          }
          @Override
          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            CronSchedule sched = ObjectUtils.tryCast(value, CronSchedule.class);
            if (sched == null) sched = EmptySchedule.INSTANCE;
            myCurrent = sched.getEditor();
            myPanel.removeAll();
            myPanel.add(myToolbar.getComponent(), BorderLayout.EAST);
            myPanel.add(myCurrent.component, BorderLayout.CENTER);
            return myPanel;
          }

          private void setCurrent(@NotNull CronSchedule schedule) {
            if (myCurrent.getter.get().getId().equals(schedule.getId())) return;
            myPanel.remove(myCurrent.component);
            myCurrent = schedule.getEditor();
            myPanel.add(myCurrent.component, BorderLayout.CENTER);
          }

          @Override
          public Object getCellEditorValue() {
            return myCurrent.getter.get();
          }
        };
      }
    }

    private class DescriptionColumnInfo extends ColumnInfo<CronTask, String> {
      public DescriptionColumnInfo() {
        super("Description");
      }

      @Override
      public @Nullable String valueOf(CronTask cronTask) {
        return cronTask.description;
      }

      @Override
      public @NotNull TableCellRenderer getRenderer(CronTask cronTask) {
        return new ColoredTableCellRenderer() {
          @Override
          protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object v, boolean b, boolean b1, int i, int i1) {
            append(v == null ? "" : v.toString());
          }
        };
      }

      @Override
      public boolean isCellEditable(CronTask cronTask) {
        return true;
      }

      @Override
      public void setValue(CronTask cronTask, String value) {
        int idx = myModel.indexOf(cronTask);
        if (idx != -1) {
          CronTask newTask = new CronTask(cronTask.id, StringUtil.nullize(value), cronTask.schedule, cronTask.action, cronTask.enabled);
          setTask(idx, newTask);
        }
      }

      @Override
      public @NotNull TableCellEditor getEditor(CronTask cronTask) {
        return new AbstractTableCellEditor() {
          private final JTextField myField = new JTextField();

          @Override
          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            myField.setText(value == null ? "" : value.toString());
            return myField;
          }

          @Override
          public Object getCellEditorValue() {
            return StringUtil.nullize(myField.getText());
          }
        };
      }
    }

    private class ActionColumnInfo extends ColumnInfo<CronTask, CronAction> {
      public ActionColumnInfo() {
        super("What");
      }

      @Override
      public @NotNull
      CronAction valueOf(CronTask cronTask) {
        return cronTask.action;
      }

      @Override
      public @NotNull TableCellRenderer getRenderer(CronTask cronTask) {
        return new ColoredTableCellRenderer() {
          {
            setFocusBorderAroundIcon(true);
          }

          @Override
          protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object v, boolean b, boolean b1, int i, int i1) {
            CronAction ac = ObjectUtils.tryCast(v, CronAction.class);
            if (ac == null) ac = EmptyAction.INSTANCE;
            setIcon(ac.getIcon(myProject));
            append(ac.getText(myProject), ac instanceof UnknownAction ? SimpleTextAttributes.GRAY_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
        };
      }

      @Override
      public boolean isCellEditable(CronTask cronTask) {
        return true;
      }

      @Override
      public void setValue(CronTask cronTask, CronAction value) {
        if (value == null) return;
        int idx = myModel.indexOf(cronTask);
        if (idx != -1) {
          CronTask newTask = new CronTask(cronTask.id, cronTask.description, cronTask.schedule, value, cronTask.enabled);
          setTask(idx, newTask);
        }
      }

      @Override
      public @NotNull
      TableCellEditor getEditor(CronTask cronTask) {
        return new AbstractTableCellEditor() {
          private final JComponent myPanel = new JPanel(new BorderLayout());
          private final JBLabel myLabel = new JBLabel();
          private CronAction myCurrent;

          {
            DumbAwareAction editAction = new DumbAwareAction(AllIcons.Actions.Edit) {
              @Override
              public void actionPerformed(@NotNull AnActionEvent e) {
                myCurrent.edit(e.getDataContext())
                  .thenAcceptAsync(c -> setCurrent(c), ApplicationManager.getApplication()::invokeLater);
              }
            };
            DumbAwareAction moreAction = new DumbAwareAction(AllIcons.Actions.MoreHorizontal) {
              @Override
              public void actionPerformed(@NotNull AnActionEvent e) {
                ListPopup popup = AddScheduleAction.createActionPopup(myProject, ac -> {
                  setCurrent(ac);
                });
                popup.showInBestPositionFor(e.getDataContext());
              }
            };
            ActionToolbar myToolbar = ActionManager.getInstance().createActionToolbar("some", new DefaultActionGroup(editAction, moreAction), true);
            myToolbar.setReservePlaceAutoPopupIcon(false);
            myToolbar.setTargetComponent(myPanel);
            myPanel.add(myLabel, BorderLayout.CENTER);
            myPanel.add(myToolbar.getComponent(), BorderLayout.EAST);
          }

          private void setCurrent(@Nullable CronAction action) {
            if (action == null) return;
            myCurrent = action;
            myLabel.setIcon(action.getIcon(myProject));
            myLabel.setText(action.getText(myProject));
          }

          @Override
          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            setCurrent(ObjectUtils.tryCast(value, CronAction.class));
            return myPanel;
          }

          @Override
          public Object getCellEditorValue() {
            return myCurrent;
          }
        };


      }
    }
  }

  public static final class State {
    List<CronTask> tasks;

    public void apply(@Nullable Project project) {
      ComponentManager container = project == null ? ApplicationManager.getApplication() : project;
      container.getService(CronDaemon.class).replaceTasks(tasks);

    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof State)) return false;
      State state = (State) o;
      return Objects.equals(tasks, state.tasks);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tasks);
    }

    public void reset(@Nullable Project project) {
      ComponentManager container = project == null ? ApplicationManager.getApplication() : project;
      tasks = container.getService(CronDaemon.class).getTasks();
   }
  }
}
