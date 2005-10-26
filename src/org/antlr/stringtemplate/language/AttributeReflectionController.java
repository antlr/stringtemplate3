package org.antlr.stringtemplate.language;

import org.antlr.stringtemplate.StringTemplate;

import java.util.*;
import java.lang.reflect.Method;

/** This class knows how to recursively walk a StringTemplate and all
 *  of its attributes to dump a type tree out.  STs contain attributes
 *  which can contain multiple values.  Those values could be
 *  other STs or have types that are aggregates (have properties).  Those
 *  types could have properties etc...
 *
 *  I am not using ST itself to print out the text for $attributes$ because
 *  it kept getting into nasty self-recursive loops that made my head really
 *  hurt.  Pretty hard to get ST to print itselt out.  Oh well, it was
 *  a cool thought while I had it.  I just dump raw text to an output buffer
 *  now.  Easier to understand also.
 */
public class AttributeReflectionController {
    /** To avoid infinite loops with cyclic type refs, track the types */
    protected Set typesVisited;

    /** Build up a string buffer as you walk the nested data structures */
    protected StringBuffer output;

    /** Can't use ST to output so must do our own indentation */
    protected int indentation=0;

    /** If in a list, itemIndex=1..n; used to print ordered list in indent().
     *  Use a stack since we have nested lists.
     */
    protected Stack itemIndexStack = new Stack();

    protected StringTemplate st;

    public AttributeReflectionController(StringTemplate st) {
        this.st = st;
    }

    public String toString() {
        typesVisited = new HashSet();
        output = new StringBuffer(1024);
        saveItemIndex();
        walkStringTemplate(st);
        restoreItemIndex();
        typesVisited = null;
        return output.toString();
    }

    public void walkStringTemplate(StringTemplate st) {
        // make sure the exact ST instance has not been visited
        if ( typesVisited.contains(st) ) {
            return;
        }
        typesVisited.add(st);
        indent();
        output.append("Template ");
        output.append(st.getName());
        output.append(":\n");
        indentation++;
        walkAttributes(st);
        indentation--;
    }

    /** Walk all the attributes in this template, spitting them out. */
    public void walkAttributes(StringTemplate st) {
        if ( st==null || st.getAttributes()==null ) {
            return;
        }
        Set keys = st.getAttributes().keySet();
        saveItemIndex();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if ( name.equals(ASTExpr.REFLECTION_ATTRIBUTES) ) {
                continue;
            }
            Object value = st.getAttributes().get(name);
            incItemIndex();
            indent();
            output.append("Attribute ");
            output.append(name);
            output.append(" values:\n");
            indentation++;
            walkAttributeValues(value);
            indentation--;
        }
        restoreItemIndex();
    }

    public void walkAttributeValues(Object attributeValue) {
        saveItemIndex();
        if ( attributeValue instanceof List ) {
            List values = (List)attributeValue;
            for (int i = 0; i < values.size(); i++) {
                Object value = (Object) values.get(i);
                Class type = value.getClass();
                incItemIndex();
                walkValue(value, type);
            }
        }
        else {
            Class type = attributeValue.getClass();
            incItemIndex();
            walkValue(attributeValue, type);
        }
        restoreItemIndex();
    }

    /** Get the list of properties by looking for get/isXXX methods.
     *  The value is the instance of "type" we are concerned with.
     *  Return null if no properties
     */
    public void walkPropertiesList(Object aggregateValue, Class type)
    {
        if ( typesVisited.contains(type) ) {
            return;
        }
        typesVisited.add(type);
        saveItemIndex();
        Method[] methods = type.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String methodName = m.getName();
            String propName = null;
            if ( methodName.length()>3 && methodName.startsWith("get") ) {
                propName =
                        Character.toLowerCase(methodName.charAt(3))+
                        methodName.substring(4,methodName.length());
            }
            else if ( methodName.length()>2 && methodName.startsWith("is") ) {
                propName =
                        Character.toLowerCase(methodName.charAt(2))+
                        methodName.substring(3,methodName.length());
            }
            else {
                continue;
            }
            incItemIndex();
            indent();
            output.append("Property ");
            output.append(propName);
            output.append(" : ");
            output.append(terseType(m.getReturnType().getName()));
            output.append("\n");
            // get actual value for STs or for Maps; otherwise type is enough
            if ( m.getReturnType()==StringTemplate.class ) {
                indentation++;
                walkStringTemplate((StringTemplate)getRawValue(aggregateValue,m));
                indentation--;
            }
            else if ( m.getReturnType()==Map.class ) {
                Map rawMap = (Map)getRawValue(aggregateValue,m);
                // if valid map instance, show key/values
                // otherwise don't descend
                if ( rawMap!=null && rawMap instanceof Map ) {
                    indentation++;
                    walkValue(
                            rawMap,
                            rawMap.getClass());
                    indentation--;
                }
            }
            else if ( !isAtomicType(m.getReturnType()) ) {
                indentation++;
                walkValue(
                        null,
                        m.getReturnType());
                indentation--;
            }
        }
        restoreItemIndex();
    }

    public void walkValue(Object value, Class type) {
        // ugh, must switch on type here; can't use polymorphism
        if ( isAtomicType(type) ) {
            walkAtomicType(type);
        }
        else if ( type==StringTemplate.class ) {
            walkStringTemplate((StringTemplate)value);
        }
        else if ( value instanceof Map ) {
            walkMap((Map)value);
        }
        else {
            walkPropertiesList(value, type);
        }
    }

    public void walkAtomicType(Class type) {
        indent();
        output.append(terseType(type.getName()));
        output.append("\n");
    }

    /** Walk all the attributes in this template, spitting them out. */
    public void walkMap(Map map) {
        if ( map==null ) {
            return;
        }
        Set keys = map.keySet();
        saveItemIndex();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if ( name.equals(ASTExpr.REFLECTION_ATTRIBUTES) ) {
                continue;
            }
            Object value = map.get(name);
            incItemIndex();
            indent();
            output.append("Key ");
            output.append(name);
            output.append(" : ");
            output.append(terseType(value.getClass().getName()));
            output.append('\n');
            if ( !isAtomicType(value.getClass()) ) {
                indentation++;
                walkValue(value,value.getClass());
                indentation--;
            }
        }
        restoreItemIndex();
    }

    /** For now, assume only java.lang stuff is atomic; collections are in
	 *  java.util.
     */
    public boolean isAtomicType(Class type) {
        return type.getName().startsWith("java.lang.");
    }

    public static String terseType(String typeName) {
        if ( typeName==null ) {
            return null;
        }
        if ( typeName.indexOf("[")>=0 ) {
            // it's an array, return whole thing
            return typeName;
        }
        int lastDot = typeName.lastIndexOf('.');
        if ( lastDot>0 ) {
            typeName = typeName.substring(lastDot+1, typeName.length());
        }
        return typeName;
    }

    /** Normally we don't actually get the value of properties since we
     *  can use normal reflection to get the list of properties
     *  of the return type.  If they as for a value, though, go get it.
     *  Need it for string template return types of properties so we
     *  can get the attribute lists.
     */
    protected Object getRawValue(Object value, Method method) {
        Object propertyValue = null;
        try {
            propertyValue = method.invoke(value,null);
        }
        catch (Exception e) {
            st.error("Can't get property "+method.getName()+" value", e);
        }
        return propertyValue;
    }

    protected void indent() {
        for (int i=1; i<=indentation; i++) {
            output.append("    ");
        }
        int itemIndex = ((Integer)itemIndexStack.peek()).intValue();
        if ( itemIndex>0 ) {
            output.append(itemIndex);
            output.append(". ");
        }
    }

    protected void incItemIndex() {
        int itemIndex = ((Integer)itemIndexStack.pop()).intValue();
        itemIndexStack.push(new Integer(itemIndex+1));
    }

    protected void saveItemIndex() {
        itemIndexStack.push(new Integer(0));
    }

    protected void restoreItemIndex() {
        itemIndexStack.pop();
    }

}
