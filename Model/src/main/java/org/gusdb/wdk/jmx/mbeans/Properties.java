package org.gusdb.wdk.jmx.mbeans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.gusdb.wdk.jmx.BeanBase;

public class Properties extends BeanBase implements DynamicMBean {

  private Map<String, String> props;

  public Properties() {
    super();
    init();
  }

  @Override
  public AttributeList getAttributes(String[] names) {
      AttributeList list = new AttributeList();
      for (String name : names) {
          String value = props.get(name);
          if (value != null)
              list.add(new Attribute(name, value));
      }
      return list;
  }

  @Override
  public String getAttribute(String name) throws AttributeNotFoundException {
    String value = props.get(name);
    if (value != null)
      return value;
    else
      throw new AttributeNotFoundException("No such property: " + name);
  }

  @Override
  public AttributeList setAttributes(AttributeList list) {
    AttributeList retlist = new AttributeList();
    Iterator<?> itr = list.iterator();
    while( itr.hasNext() ) {
      Attribute attr = (Attribute)itr.next();
      String name = attr.getName();
      Object value = attr.getValue();
      if (props.get(name) != null && value instanceof String) {
          props.put(name, (String) value);
          retlist.add(new Attribute(name, value));
      }
    }
    return retlist;
  }

  @Override
  public void setAttribute(Attribute attribute) 
  throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
    String name = attribute.getName();
    if (props.get(name) == null)
        throw new AttributeNotFoundException(name);
    Object value = attribute.getValue();
    if (!(value instanceof String)) {
        throw new InvalidAttributeValueException(
                "Attribute value not a string: " + value);
    }
    props.put(name, (String) value);
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    ArrayList<String> names = new ArrayList<String>();
    for (Object name : props.keySet()) {
      names.add((String) name);
    }
    MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[names.size()];
    Iterator<String> it = names.iterator();
    for (int i = 0; i < attrs.length; i++) {
      String name = it.next();
      attrs[i] = new MBeanAttributeInfo(
              name,
              "java.lang.String",
              name,
              true,    // isReadable
              false,   // isWritable
              false);  // isIs
    }

    MBeanOperationInfo[] opers = {
      new MBeanOperationInfo(
              "reload",
              "Reload properties from model",
              null,
              "void",
              MBeanOperationInfo.ACTION)
    };

    return new MBeanInfo(
            this.getClass().getName(),
            "WDK Properties MBean",
            attrs,
            null,  // constructors
            opers,  // operators
            null); // notifications
  }

  @Override
  public Object invoke(String name, Object[] args, String[] sig) 
  throws MBeanException, ReflectionException {
    if (name.equals("reload") &&
            (args == null || args.length == 0) &&
            (sig == null || sig.length == 0)) {
        try {
          init();
          return null;
        } catch (Exception e) {
          throw new MBeanException(e);
        }
    }
    throw new ReflectionException(new NoSuchMethodException(name));
  }

  private void init() {
    props = getWdkModel().getProperties();
  }
}
