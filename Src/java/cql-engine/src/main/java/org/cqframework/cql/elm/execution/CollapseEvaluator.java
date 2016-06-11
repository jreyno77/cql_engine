package org.cqframework.cql.elm.execution;

import org.cqframework.cql.execution.Context;
import org.cqframework.cql.runtime.Interval;
import org.cqframework.cql.runtime.Quantity;
import org.cqframework.cql.runtime.Value;
import java.math.BigDecimal;
import java.util.*;

/**
* Created by Chris Schuler on 6/8/2016
*/
public class CollapseEvaluator extends Collapse {

  public static Object collapse(ArrayList intervals) {
    Collections.sort(intervals, new Comparator<Interval>() {
      @Override
      public int compare(Interval o1, Interval o2) {
        if (o1.getStart() instanceof Integer) {
          if (Value.compareTo(o1.getStart(), o2.getStart(), "<")) { return -1; }
          else if (Value.compareTo(o1.getStart(), o2.getStart(), "==")) { return 0; }
          else if (Value.compareTo(o1.getStart(), o2.getStart(), ">")) { return 1; }
        }
        else if (o1.getStart() instanceof BigDecimal) {
          return ((BigDecimal)o1.getStart()).compareTo((BigDecimal)o2.getStart());
        }
        else if (o1.getStart() instanceof Quantity) {
          return (((Quantity)o1.getStart()).getValue().compareTo(((Quantity)o2.getStart()).getValue()));
        }

        throw new IllegalArgumentException(String.format("Cannot CollapseEvaluator arguments of type '%s' and '%s'.", o1.getClass().getName(), o1.getClass().getName()));
      }
    });

    for (int i = 0; i < intervals.size(); ++i) {
      if ((i+1) < intervals.size()) {
        if (OverlapsEvaluator.overlaps((Interval)intervals.get(i), (Interval)intervals.get(i+1))) {
          intervals.set(i, new Interval(((Interval)intervals.get(i)).getStart(), true, ((Interval)intervals.get(i+1)).getEnd(), true));
          intervals.remove(i+1);
          i -= 1;
        }
      }
    }
    return intervals;
  }

  @Override
  public Object evaluate(Context context) {
    Iterable<Object> list = (Iterable<Object>)getOperand().evaluate(context);
    if (list == null) { return null; }
    ArrayList intervals = new ArrayList();
    for (Object interval : list) {
      if (interval != null) { intervals.add(interval); }
    }
    if (intervals.size() == 1) { return intervals; }

    return collapse(intervals);
  }
}
