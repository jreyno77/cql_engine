package org.cqframework.cql.elm.execution;

import org.cqframework.cql.execution.Context;
import org.cqframework.cql.runtime.Quantity;
import org.cqframework.cql.runtime.DateTime;

import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;

/*
*** NOTES FOR DATETIME ***
The subtract (-) operator returns the value of the given date/time, decremented by the time-valued quantity,
  respecting variable length periods for calendar years and months.
For DateTime values, the quantity unit must be one of: years, months, days, hours, minutes, seconds, or milliseconds.
For Time values, the quantity unit must be one of: hours, minutes, seconds, or milliseconds.
The operation is performed by attempting to derive the highest granularity precision first, working down successive
  granularities to the granularity of the time-valued quantity. For example, the following subtraction:
    DateTime(2014) - 24 months
    This example results in the value DateTime(2012) even though the date/time value is not specified to the level of precision of the time-valued quantity.
If either argument is null, the result is null.
NOTE: see note in AddEvaluator
*/

/**
 * Created by Bryn on 5/25/2016 (v1)
 * Edited by Chris Schuler on 6/14/2016 (v2), 6/25/2016 (v3)
 */
public class SubtractEvaluator extends Subtract {

  private static final int YEAR_RANGE_MIN = 0001;

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);

        if (left == null || right == null) {
            return null;
        }

        // -(Integer, Integer)
        if (left instanceof Integer) {
            return (Integer)left - (Integer)right;
        }

        // -(Decimal, Decimal)
        else if (left instanceof BigDecimal) {
            return ((BigDecimal)left).subtract((BigDecimal)right);
        }

        // -(Quantity, Quantity)
        else if (left instanceof Quantity) {
          return (((Quantity)left).getValue()).subtract(((Quantity)right).getValue());
        }

        // -(DateTime, Quantity)
        else if (left instanceof DateTime && right instanceof Quantity) {
          DateTime dt = (DateTime)left;
          String unit = ((Quantity)right).getUnit();
          int value = ((Quantity)right).getValue().intValue();

          int idx = DateTime.getFieldIndex2(unit);

          if (idx != -1) {
            int startSize = dt.getPartial().size();
            // check that the Partial has the precision specified
            if (startSize < idx + 1) {
              // expand the Partial to the proper precision
              for (int i = startSize; i < idx + 1; ++i) {
                dt.setPartial(dt.getPartial().with(DateTime.getField(i), DateTime.getField(i).getField(null).getMinimumValue()));
              }
            }

            // do the subtraction
            dt.setPartial(dt.getPartial().property(DateTime.getField(idx)).addToCopy(-value));
            // truncate until non-minimum value is found
            for (int i = idx; i >= startSize; --i) {
              if (dt.getPartial().getValue(i) > DateTime.getField(i).getField(null).getMinimumValue()) {
                break;
              }
              dt.setPartial(dt.getPartial().without(DateTime.getField(i)));
            }
          }

          else {
            throw new IllegalArgumentException(String.format("Invalid duration unit: %s", unit));
          }
          if (dt.getPartial().getValue(0) < YEAR_RANGE_MIN) {
            throw new ArithmeticException("The date time addition results in a year less than the accepted range.");
          }

          return dt;
        }

        // TODO: Finish implementation of Subtract
        // -(Time, Quantity)

        throw new IllegalArgumentException(String.format("Cannot %s arguments of type '%s' and '%s'.", this.getClass().getSimpleName(), left.getClass().getName(), right.getClass().getName()));
    }
}
