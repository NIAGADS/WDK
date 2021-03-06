package org.gusdb.wdk.jmx.mbeans;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

import org.apache.log4j.Logger;
import org.gusdb.wdk.jmx.BeanBase;

/**
 * Abstract class for a dynamic MBean that represents data as stored in
 * instantiated WDK class objects. AbstractConfig does not read from
 * configuration files, but from Java class accessors.
 */
public abstract class AbstractAttributesBean extends BeanBase implements DynamicMBean {

  private static final Logger LOG = Logger.getLogger(AbstractAttributesBean.class);

  private HashMap<String, String> _props = new HashMap<String, String>();

  /**
   * Initialize a HashMap by acquiring a configuration object
   * and passing it to setValuesFromGetters();
   */
  protected abstract void init();

  /**
   * implementation of DynamicMBean interface method
   */
  @Override
  public AttributeList getAttributes(String[] names) {
    AttributeList list = new AttributeList();
    for (String name : names) {
      String value = _props.get(name);
      if (value != null)
        list.add(new Attribute(name, value));
    }
    return list;
  }

  /**
   * implementation of DynamicMBean interface method
   */
  @Override
  public String getAttribute(String name) throws AttributeNotFoundException {
    String value = _props.get(name);
    if (value != null)
      return value;
    else
      throw new AttributeNotFoundException("No such property: " + name);
  }

  /**
   * implementation of DynamicMBean interface method
   */
  @Override
  public AttributeList setAttributes(AttributeList list) {
    AttributeList retlist = new AttributeList();
    Iterator<?> itr = list.iterator();
    while( itr.hasNext() ) {
      Attribute attr = (Attribute)itr.next();
      String name = attr.getName();
      Object value = attr.getValue();
      if (_props.get(name) != null && value instanceof String) {
          _props.put(name, (String) value);
          retlist.add(new Attribute(name, value));
      }
    }
    return retlist;
  }

  /**
   * implementation of DynamicMBean interface method
   */
  @Override
  public void setAttribute(Attribute attribute)
            throws InvalidAttributeValueException, MBeanException,
                   AttributeNotFoundException {
    String name = attribute.getName();
    if (_props.get(name) == null)
        throw new AttributeNotFoundException(name);
    Object value = attribute.getValue();
    if (!(value instanceof String)) {
        throw new InvalidAttributeValueException(
                "Attribute value not a string: " + value);
    }
    _props.put(name, (String) value);
  }

  /**
   * implementation of DynamicMBean interface method
   */
  @Override
  public MBeanInfo getMBeanInfo() {
    ArrayList<String> names = new ArrayList<String>();
    for (Object name : _props.keySet()) {
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
              "Reload configuration from model",
              null,
              "void",
              MBeanOperationInfo.ACTION)
    };

    return new MBeanInfo(
            this.getClass().getName(),
            "WDK model-config.xml MBean",
            attrs,
            null,  // constructors
            opers,  // operators
            null); // notifications
  }

  /**
   * implementation of DynamicMBean interface method
   */
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

  /**
   * Use introspection to find and call getter methods in given configuration
   * object. The data is put into a map where the key name is derived from
   * the getter method, sans 'get', and the value is the return value of the
   * getter method.
   *
   * If a non-null string is given for the section parameter it will be prepended in square brackets to the
   * generated key name. e.g.
   * <pre>
   * {@code
   * <modelConfig modelName=CryptoDB>
   *   <appDB url="cryp"/>
   * </model>
   * }</pre>
   *
   * can be represented as
   *
   * <pre>{@code
   * [appDb] connectionUrl = cryp
   * [global] modelName = CryptoDB
   * }</pre>
   *
   * This allows multidimensional configuration file structures to be
   * stored flat and therefore more readable in JMX consoles.
   * Used wisely, the bracketed section names can leveraged to reconstruct
   * a multidimensional configuration in custom code.
   *
   * Values for keys that look like passwords are masked.
   *
   * @param section A string or null
   * @param config A configuration object that has get methods of interest.
   */
  protected void setValuesFromGetters(String section, Object config) {
    if (config == null) {
      LOG.debug("skipping null config: '" + section + "'");
      return;
    }
    try {
      Class<?> c = Class.forName(config.getClass().getName());
      Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
          Method method = methods[i];
          String mname = method.getName();

            if ((method.getDeclaringClass().getName().startsWith("org.gusdb.wdk.model.") ||
                method.getDeclaringClass().getName().startsWith("org.apidb.apicommon.model."))
                && mname.startsWith("get")) {
            // remove 'get', lowercase first letter
            String key = Character.toLowerCase(mname.charAt(3)) + mname.substring(4);

            Object value = null;
            try {
                value = method.invoke(config);
            } catch (Exception e) {
                LOG.debug("skipping, could not invoke method: '" +
                  mname + "' of config '" +
                  method.getDeclaringClass().getName() + "'.");
                continue;
            }

            if ( value == null ||
                  !(value.getClass().getName().startsWith("java.lang.")) )
                      continue;

            if ( value instanceof String &&
                 (
                   key.toLowerCase().contains("password") ||
                   key.toLowerCase().contains("passwd") ||
                   key.toLowerCase().contains("passphrase") ||
                   key.toLowerCase().contains("secret")
                 )
               ) { value = "*****"; }
            LOG.debug("config key '" + key +
                         "', config value '" + value + "'");
            String prefix = (section != null) ? "[" + section + "] " : "";
            _props.put(prefix + key, value.toString());
          }
        }
    } catch (Exception e) {
      LOG.fatal(e);
    }
  }

}
