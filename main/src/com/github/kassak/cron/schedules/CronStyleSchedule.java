package com.github.kassak.cron.schedules;

import com.github.kassak.cron.CronDaemon;
import com.github.kassak.cron.CronSchedule;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.FastUtilHashingStrategies;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.concurrent.ScheduledFuture;

public class CronStyleSchedule implements CronSchedule {
  private final CronExpr myExpr;
  private CronExpr.Compiled myCompiled;

  public CronStyleSchedule(@NotNull CronExpr expr) {
    this.myExpr = expr;
  }

  @Override
  public ScheduledFuture<?> schedule(@NotNull CronDaemon daemon, @NotNull Runnable r) {
    if (myCompiled == null) myCompiled = myExpr.compile();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime next = myCompiled.next(now);
    System.out.println("now is -> " + now + " -> " + next);
    return daemon.scheduleAt(r, next);
  }

  public static class CronExpr {
    public final String sec;
    public final String min;
    public final String hour;
    public final String monthDay;
    public final String month;
    public final String weekDay;

    public CronExpr(String sec, String min, String hour, String monthDay, String month, String weekDay) {
      this.sec = sec;
      this.min = min;
      this.hour = hour;
      this.monthDay = monthDay;
      this.month = month;
      this.weekDay = weekDay;
    }

    public Compiled compile() {
      return new Compiled(
        collect(sec, 60, 0, null),
        collect(min, 60, 0, null),
        collect(hour, 24, 0, null),
        collect(monthDay, 31, 1, null),
        collect(month, 12, 1, Compiled.MON_MAP),
        collect(weekDay, 7, 0, Compiled.WD_MAP)
      );
    }

    public static int[] collect(String s, int modulo, int base, @Nullable Object2IntMap<String> wordParser) {
      IntList numbers = new IntArrayList();
      for (String rg : StringUtil.tokenize(s, ",")) {
        if (rg.equals("*")) return ArrayUtil.EMPTY_INT_ARRAY;
        int rgMark = rg.indexOf('-');
        if (rgMark != -1) {
          appendRange(numbers, rg.substring(0, rgMark), rg.substring(rgMark + 1), wordParser);
        }
        else {
          if (modulo != 0 && rg.startsWith("*/")) {
            String stepStr = rg.substring(2);
            appendStep(numbers, stepStr, base, modulo);
          }
          else {
            numbers.add(parseNumber(rg, wordParser));
          }
        }
      }
      numbers.sort(IntComparators.NATURAL_COMPARATOR);
      return numbers.toIntArray();
    }

    private static void appendStep(IntList numbers, String stepStr, int base, int modulo) {
      int step = parseNumber(stepStr, null);
      if (step < 1) throw new IllegalArgumentException("Wrong step value " + step);
      for (int i = 0; i < modulo; i += step) {
        numbers.add(base + i);
      }
    }

    private static void appendRange(IntList numbers, String from, String to, @Nullable Object2IntMap<String> wordParser) {
      int start = parseNumber(from, wordParser);
      int end = parseNumber(to, wordParser);
      for (int i = start; i <= end; ++i) {
        numbers.add(i);
      }
    }

    private static int parseNumber(String v, @Nullable Object2IntMap<String> wordParser) {
      try {
        return Integer.parseInt(v);
      }
      catch (NumberFormatException e) {
        int n = wordParser == null ? -1 : wordParser.getOrDefault(v, -1);
        if (n != -1) return n;
        throw e;
      }
    }

    public static class Compiled {
      public final int[] sec;
      public final int[] min;
      public final int[] hour;
      public final int[] monthDay;
      public final int[] month;
      public final int[] weekDay;

      public Compiled(int[] sec, int[] min, int[] hour, int[] monthDay, int[] month, int[] weekDay) {
        this.sec = sec;
        this.min = min;
        this.hour = hour;
        this.monthDay = monthDay;
        this.month = month;
        this.weekDay = weekDay;
      }

      public LocalDateTime next(LocalDateTime from) {
        return align(from.plus(1500, ChronoUnit.MILLIS).with(ChronoField.MICRO_OF_SECOND, 0));
      }

      private LocalDateTime align(LocalDateTime dt) {
        Ref<LocalDateTime> ref = Ref.create(dt);
        while(true) {
          if (alignStep(ref)) {
            return ref.get();
          }
        }
      }

      @SuppressWarnings("RedundantIfStatement")
      private boolean alignStep(Ref<LocalDateTime> toAlign) {
        if (!alignMonth(toAlign)) return false;
        if (!alignDay(toAlign)) return false;
        if (!alignHour(toAlign)) return false;
        if (!alignMinute(toAlign)) return false;
        if (!alignSecond(toAlign)) return false;
        return true;
      }

      private boolean alignMonth(Ref<LocalDateTime> ref) {
        return alignField(ref, ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, month);
      }

      private boolean alignHour(Ref<LocalDateTime> ref) {
        return alignField(ref, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, hour);
      }

      private boolean alignMinute(Ref<LocalDateTime> ref) {
        return alignField(ref, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, min);
      }

      private boolean alignSecond(Ref<LocalDateTime> ref) {
        return alignField(ref, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, sec);
      }

      private boolean alignDay(Ref<LocalDateTime> ref) {
        if (weekDay.length == 0) return alignMonthDay(ref);
        if (monthDay.length == 0) return alignWeekDay(ref);
        Ref<LocalDateTime> md = Ref.create(ref.get());
        boolean mdr = alignMonthDay(md);
        Ref<LocalDateTime> wd = Ref.create(ref.get());
        boolean wdr = alignWeekDay(wd);
        if (!mdr || !wdr) {
          ref.set((mdr ? md : wd).get());
          return mdr || wdr;
        }
        ref.set(md.get().getDayOfMonth() < wd.get().getDayOfMonth() ? md.get() : wd.get());
        return true;
      }

      private boolean alignMonthDay(Ref<LocalDateTime> ref) {
        return alignField(ref, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, monthDay);
      }

      private boolean alignWeekDay(Ref<LocalDateTime> ref) {
        LocalDateTime prev = ref.get();
        boolean res = alignField(ref, ChronoField.ALIGNED_WEEK_OF_MONTH, ChronoField.DAY_OF_WEEK, weekDay);
        if (prev.get(ChronoField.MONTH_OF_YEAR) != ref.get().get(ChronoField.MONTH_OF_YEAR)) {
          ref.set(addAndReset(ref.get(), ChronoField.MONTH_OF_YEAR));
          return false;
        }
        return res;
      }

      private boolean alignField(Ref<LocalDateTime> ref, ChronoField parentField, ChronoField field, int[] values) {
        LocalDateTime dt = ref.get();
        int curVal = dt.get(field);
        int aVal = align(curVal, values);
        if (aVal == curVal) return true;
        if (aVal != -1) {
          LocalDateTime newDate = resetBefore(dt, field).with(field, aVal);
          if (newDate.get(field) == aVal && dt.get(parentField) == newDate.get(parentField)) { // for months 31 -> 28/29/30
            ref.set(newDate);
            return true;
          }
        }
        ref.set(addAndReset(dt, parentField));
        return false;
      }

      @NotNull
      private LocalDateTime addAndReset(LocalDateTime dt, ChronoField field) {
        return resetBefore(
          dt.plus(1, field.getBaseUnit()),
          field
        );
      }

      private static final TemporalField[] resetOrder = {
        ChronoField.SECOND_OF_MINUTE,
        ChronoField.MINUTE_OF_HOUR,
        ChronoField.HOUR_OF_DAY,
        ChronoField.DAY_OF_MONTH,
        ChronoField.MONTH_OF_YEAR,
        ChronoField.YEAR,
      };
      private static final TemporalField[] resetWdOrder = {
        ChronoField.SECOND_OF_MINUTE,
        ChronoField.MINUTE_OF_HOUR,
        ChronoField.HOUR_OF_DAY,
        ChronoField.DAY_OF_WEEK,
        ChronoField.ALIGNED_WEEK_OF_YEAR,
      };

      @NotNull
      private LocalDateTime resetBefore(LocalDateTime d, TemporalField field) {
        TemporalField[] order = field == ChronoField.ALIGNED_WEEK_OF_YEAR || field == ChronoField.DAY_OF_WEEK
          ? resetWdOrder
          : resetOrder;
        LocalDateTime res = d;
        for (TemporalField rf : order) {
          if (rf == field) break;
          res = res.with(rf, rf.range().getMinimum());
        }
        return res;
      }

      private int align(int v, int[] values) {
        if (values.length == 0) return v;
        for (int value : values) {
          if (v <= value) return value;
        }
        return -1;
      }

      private static final Object2IntMap<String> WD_MAP = index(
        "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
      private static final Object2IntMap<String> MON_MAP = index(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

      private static Object2IntMap<String> index(String... s) {
        Object2IntMap<String> res = new Object2IntOpenCustomHashMap<>(
          FastUtilHashingStrategies.getCharSequenceStrategy(false));
        for (int i = 0, sLength = s.length; i < sLength; i++) {
          res.put(s[i], i);
        }
        return res;
      }
    }

  }
}
