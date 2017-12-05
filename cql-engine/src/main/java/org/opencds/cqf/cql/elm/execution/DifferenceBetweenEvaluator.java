package org.opencds.cqf.cql.elm.execution;

import org.joda.time.*;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.*;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.opencds.cqf.cql.runtime.BaseTemporal.truncatePartial;
import static org.opencds.cqf.cql.runtime.Uncertainty.resolveUncertaintyWithFunction;

// for Uncertainty

/*
difference in precision between(low DateTime, high DateTime) Integer
difference in precision between(low Time, high Time) Integer

The difference-between operator returns the number of boundaries crossed for the specified precision between the
first and second arguments.
If the first argument is after the second argument, the result is negative.
The result of this operation is always an integer; any fractional boundaries are dropped.
For DateTime values, precision must be one of: years, months, days, hours, minutes, seconds, or milliseconds.
For Time values, precision must be one of: hours, minutes, seconds, or milliseconds.
If either argument is null, the result is null.

Additional Complexity: precison elements above the specified precision must also be accounted for (handled by Joda Time).
For example:
days between DateTime(2012, 5, 5) and DateTime(2011, 5, 0) = 365 + 5 = 370 days

NOTE: This is the same operation as DurationBetween, but the precision after the specified precision is truncated
to get the number of boundaries crossed instead of whole calendar periods.
For Example:
difference in days between DateTime(2014, 5, 12, 12, 10) and DateTime(2014, 5, 25, 15, 55)
will truncate the DateTimes to:
DateTime(2014, 5, 12) and DateTime(2014, 5, 25) respectively
*/

/**
 * Created by Chris Schuler on 6/22/2016
 */
public class DifferenceBetweenEvaluator extends org.cqframework.cql.elm.execution.DifferenceBetween {

    public static Integer betweenMillis(Partial leftTrunc, Partial rightTrunc, org.joda.time.DateTime leftDateTime,
                                            org.joda.time.DateTime rightDateTime, int idx, boolean dt)
    {
        return dt ? Seconds.secondsBetween(leftDateTime, rightDateTime).getSeconds() * 1000 + rightTrunc.getValue(idx) - leftTrunc.getValue(idx)
                  : Seconds.secondsBetween(leftDateTime, rightDateTime).getSeconds() * 1000 + rightTrunc.getValue(idx - 3) - leftTrunc.getValue(idx - 3);
    }

    public static Integer between(org.joda.time.DateTime leftDateTime, org.joda.time.DateTime rightDateTime, int idx) {
        Integer ret = 0;
        Instant leftInstant = leftDateTime.toInstant();
        Instant rightInstant = rightDateTime.toInstant();
        switch(idx) {
            case 0: ret = Years.yearsBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getYears();
                break;
            case 1: ret = Months.monthsBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getMonths();
                break;
            case 2: ret = Days.daysBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getDays();
                break;
            case 3: ret = Hours.hoursBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getHours();
                break;
            case 4: ret = Minutes.minutesBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getMinutes();
                break;
            case 5: ret = Seconds.secondsBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getSeconds();
                break;
            case 7: ret = Days.daysBetween(leftDateTime.toInstant(), rightDateTime.toInstant()).getDays() / 7;
        }
        if (idx < 6) {
            while (++idx < 7) {
                if (leftInstant.get(DateTime.getField(idx)) > rightInstant.get(DateTime.getField(idx))) {
                    ret += 1;
                    break;
                }
            }
        }
        return ret;
    }

    public static Integer between(BaseTemporal left, BaseTemporal right, int idx, boolean dt) {
        if (idx == 6) {
            return betweenMillis(left.getPartial(), right.getPartial(), left.toJodaDateTime(), right.toJodaDateTime(), idx, dt);
        }
        return between(left.toJodaDateTime(), right.toJodaDateTime(), idx);
    }

    public static Object difference(Object left, Object right, String precision)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (precision == null) {
            throw new IllegalArgumentException("Precision must be specified.");
        }

        if (left == null || right == null) {
            return null;
        }

        boolean isDateTime = left instanceof DateTime;

        BaseTemporal leftTemporal = (BaseTemporal) left;
        BaseTemporal rightTemporal = (BaseTemporal) right;

        int index = isDateTime ? DateTime.getFieldIndex(precision) : Time.getFieldIndex(precision);

        if (index != -1) {
            boolean weeks = false;
            if (index == 7) {
                index = 2;
                weeks = true;
            }

            // Uncertainty
            if (Uncertainty.isUncertain(leftTemporal, precision) || Uncertainty.isUncertain(rightTemporal, precision)) {
                DifferenceBetweenEvaluator evaluator = new DifferenceBetweenEvaluator();
                Method method = evaluator.getClass().getMethod("between", BaseTemporal.class, BaseTemporal.class, int.class, boolean.class);
                return resolveUncertaintyWithFunction(leftTemporal, rightTemporal, precision, evaluator, method, index);
            }

            // truncate Partial
            Partial leftTrunc = truncatePartial(leftTemporal, index);
            Partial rightTrunc = truncatePartial(rightTemporal, index);

            if (weeks) {
                index = 7;
            }

            org.joda.time.DateTime leftDateTime = isDateTime ? ((DateTime) leftTemporal).getJodaDateTime() : leftTrunc.toDateTime(new org.joda.time.DateTime(leftTemporal.getChronology()));
            org.joda.time.DateTime rightDateTime = isDateTime ? ((DateTime) rightTemporal).getJodaDateTime() : rightTrunc.toDateTime(new org.joda.time.DateTime(rightTemporal.getChronology()));

            if (!isDateTime) {
                index += 3;
            }

            if (index == 6) {
                return betweenMillis(leftTrunc, rightTrunc, leftDateTime, rightDateTime, index, isDateTime);
            }

            return between(leftDateTime, rightDateTime, index);
        }

        else {
            throw new IllegalArgumentException(String.format("Invalid duration precision: %s", precision));
        }
    }

    @Override
    public Object evaluate(Context context) {
        Object left = getOperand().get(0).evaluate(context);
        Object right = getOperand().get(1).evaluate(context);
        String precision = getPrecision().value();

        try {
            return context.logTrace(this.getClass(), difference(left, right, precision), left, right);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
