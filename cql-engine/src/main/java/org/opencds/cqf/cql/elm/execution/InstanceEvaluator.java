package org.opencds.cqf.cql.elm.execution;

import org.opencds.cqf.cql.execution.Context;

/**
 * Created by Bryn on 5/25/2016.
 */
public class InstanceEvaluator extends org.cqframework.cql.elm.execution.Instance {

    @Override
    public Object evaluate(Context context) {
//        String type =
//        if (context.resolveType(this.getClassType()).getName().contains("$")) {
//
//        }

        Class clazz = context.resolveType(this.getClassType());
        try {
            Object object = clazz.newInstance();
            for (org.cqframework.cql.elm.execution.InstanceElement element : this.getElement()) {
                Object value = element.getValue().evaluate(context);
                context.setValue(object, element.getName(), value);
            }

            return object;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Could not create an instance of class %s.", clazz.getName()));
        }
//        catch (NoSuchFieldException e) {
//            throw new IllegalArgumentException(String.format("The field %s does not exist in class %s", this.getElement().get(0).getName(), clazz.getSimpleName()));
//        }
    }
}
