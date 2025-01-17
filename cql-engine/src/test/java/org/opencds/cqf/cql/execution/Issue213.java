package org.opencds.cqf.cql.execution;
import org.opencds.cqf.cql.elm.execution.EquivalentEvaluator;
import org.opencds.cqf.cql.runtime.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Issue213 extends CqlExecutionTestBase {
    @Test
    public void testInterval() {
        Context context = new Context(library);
        BigDecimal offset = TemporalHelper.getDefaultOffset();
        Object result = context.resolveExpressionRef("Collapsed Treatment Intervals").getExpression().evaluate(context);
        Assert.assertTrue(EquivalentEvaluator.equivalent(((Interval)((List)result).get(0)).getStart(), new DateTime(offset, 2018, 1, 1)));
        Assert.assertTrue(EquivalentEvaluator.equivalent(((Interval)((List)result).get(0)).getEnd(), new DateTime(offset, 2018, 8, 28)));
        Assert.assertTrue(EquivalentEvaluator.equivalent(((Interval)((List)result).get(1)).getStart(), new DateTime(offset, 2018, 8, 30)));
        Assert.assertTrue(EquivalentEvaluator.equivalent(((Interval)((List)result).get(1)).getEnd(), new DateTime(offset, 2018, 10, 15)));
        assertThat(((List)result).size(), is(2));
    }
}
