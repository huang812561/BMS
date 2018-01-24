package com.bms.util.methodreflect;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bms.util.commmon.StringUtil;



/***
 * 
 * @Description:  类型元数据
 * @author 黄国强
 * @date 2017年5月10日
 *
 * @param <T>
 */
public final class MetaObject<T>
{
    /** Log */
    private static final Logger log = LoggerFactory.getLogger(MetaObject.class); // NOPMD

    /** Field Cache */
    //private transient Map<String, Field> fieldCache = new HashMap<String, Field>();

    /** Method Cache */
    //private transient Map<String, Method> methodCache = new HashMap<String, Method>();

    /** 目标对象 */
    private transient T target;

    /** 类对象 */
    private transient Class<?> clazz;

    /**
     * 以目标对象构建元数据。
     *
     * @param target 目标对象
     */
    private MetaObject(final T target)
    {
        this.clazz = target.getClass();
        this.target = target;
    }

    /**
     * 从指定的目标对象中获取元数据。
     *
     * @param target 目标对象
     * @param <T> 目标类型
     * 
     * @return 该对象的元数据
     */
    public static <T> MetaObject<T> fromObject(final T target)
    {
        return new MetaObject<T>(target);
    }

    /**
     * 获取目标对象。
     *
     *
     * @return 目标对象
     */
    public T getTarget()
    {
        return target;
    }

    /**
     * 给指定属性赋值并返回旧值
     *
     * 如果指定属性不存在或不可访问，则赋值失败，但不会抛出任何异常。<br>
     * 无论赋值成功与否，此方法都将尽最大努力返回赋值之前的属性值。
     * <i>赋值有风险，操作须谨慎。</i>
     *
     * @param property 属性名
     * @param value 属性值
     *
     * @return 旧属性值
     */
    public Object setValue(final String property, final Object value)
    {
        Object previous = getValue(property);

        // 拼凑赋值器名,尝试使用公开的赋值器
        boolean success = false;
        Method method = setter(property);
        if (method != null)
        {
            try
            {
                method.invoke(target, value);
                success = true; // 标记方法调用成功，将不会通过属性强行赋值
            }
            catch (IllegalArgumentException e)
            {
                ReflectLogHelper.logIllegalArgument(log, method, value);
            }
            catch (IllegalAccessException e)
            {
                ReflectLogHelper.logIllegalAccess(log, method);
            }
            catch (InvocationTargetException e)
            {
                ReflectLogHelper.logInvocationTarget(log, method, target);
            }
        }

        // 若方法调用失败，则尝试使用暴力
        if (!success)
        {
            Field field = searchField(property);
            if (null != field)
            {
                checkAccess(field);

                try
                {
                    field.set(target, value);
                }
                catch (IllegalArgumentException e)
                {
                    ReflectLogHelper.logIllegalArgument(log, method, value);
                }
                catch (IllegalAccessException e)
                {
                    ReflectLogHelper.logIllegalAccess(log, method);
                }
            }
        }

        return previous;
    }

    /**
     * 获取指定的属性值
     *
     * 如果指定属性不存在或不可访问，则获取失败，但不会抛出任何异常。
     * <i>取值有风险，操作须谨慎。</i>
     *
     * @param property 属性名
     *
     * @return 属性值
     */
    public Object getValue(final String property)
    {
        // 尝试使用公开的取值器
        boolean success = false;
        Object result = null;
        Method method = getter(property);
        if (method != null)
        {
            try
            {
                result = method.invoke(target);
                success = true; // 标记方法调用成功，将不会通过属性强行赋值
            }
            catch (IllegalArgumentException e)
            {
                ReflectLogHelper.logIllegalArgument(log, method, null);
            }
            catch (IllegalAccessException e)
            {
                ReflectLogHelper.logIllegalAccess(log, method);
            }
            catch (InvocationTargetException e)
            {
                ReflectLogHelper.logInvocationTarget(log, method, target);
            }
        }

        // 若方法调用失败，则尝试使用暴力
        if (!success)
        {
            Field field = searchField(property);
            if (null != field)
            {
                checkAccess(field);

                try
                {
                    result = field.get(target);
                }
                catch (IllegalArgumentException e)
                {
                    ReflectLogHelper.logIllegalArgument(log, method, null);
                }
                catch (IllegalAccessException e)
                {
                    ReflectLogHelper.logIllegalAccess(log, method);
                }
            }
        }

        return result;
    }

    /**
     * 测试是否含有指定属性
     *
     * @param property 属性名
     * @return true如果有的话
     */
    public boolean hasFiled(final String property)
    {
        return searchField(property) != null;
    }

    /**
     * 获取指定属性的类型
     *
     * @param property 属性名
     * 
     * @return 属性类型如果指定属性不存在，则返回null
     */
    public Class<?> getFiledType(final String property)
    {
        Class<?> type = null;
        Field filed = searchField(property);
        if (filed != null)
        {
            type = filed.getType();
        }

        return type;
    }

    /**
     * 获得取值器。
     *
     * @param property 属性名
     * @return 若成功，则返回参数给定属性的取值器，否则返回null
     */
    public Method getter(final String property)
    {
        Method method = null;
        if (StringUtil.isNotEmpty(property))
        {
            // 尝试使用公开的取值器
            String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
            String getterName = "get" + suffix;
            method = searchMethod(getterName);

            if (method == null)
            {
                /*
                 *  做最后的挣扎
                 *  测试该属性是否为boolean型，
                 *  如果是的话，就尝试isXxx形式的取值器名
                 */
                Field field = searchField(property);
                if (null != field)
                {
                    Class<?> type = field.getType();
                    if (type == boolean.class || type == Boolean.class)
                    {
                        getterName = "is" + suffix;
                        method = searchMethod(getterName);
                    }
                }
            }
        }

        return method;
    }

    /**
     * 获取赋值器。
     * 
     * @param property 属性名
     * 
     * @return 若成功，则返回参数给定属性的赋值器，否则返回null
     */
    public Method setter(final String property)
    {
        Method method = null;
        if (StringUtil.isNotEmpty(property))
        {
            Field field = searchField(property);
            if (null != field)
            {
                // 拼凑取值器名
                Class<?> type = field.getType();
                String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
                String setterName = "set" + suffix;
                method = searchMethod(setterName, type);
            }
        }

        return method;
    }

    /**
     * 检查成员的访问性。
     *
     * 如果是非公开的，则尝试暴力破解访问限制
     *
     * @param member 类成员
     * 
     * @return 是否成功破解
     */
    private boolean checkAccess(final AccessibleObject member)
    {
        boolean success = true;
        if (!member.isAccessible())
        {
            try
            {
                member.setAccessible(true); // 暴力破解访问限制
            }
            catch (SecurityException e)
            {
                // 发生安全异常，放弃
                success = false;
                ReflectLogHelper.logSecurity(log, e.getMessage());
            }
        }

        return success;
    }

    /**
     * 搜寻Field
     *
     * 搜寻指定类型中指定名称的数据域
     * 向上追溯直到Object类
     * 有属性覆盖的，先到先得
     *
     * @param property 被搜寻的域名
     * @return 实际找到的域，若找不到，则返回null
     */
//    @Deprecated
//    private Field searchField4Cache(final String property)
//    {
//        String className = clazz.getName();
//        String cacheKey = className + "." + property;
//        Field field = null;
//        if (fieldCache.containsKey(cacheKey))
//        {
//            field = fieldCache.get(cacheKey);
//        }
//        else
//        {
//            Class<?> startClass = clazz;
//            while (null != startClass)
//            {
//                try
//                {
//                    field = startClass.getDeclaredField(property);
//                    break;
//                }
//                catch (NoSuchFieldException e)
//                {
//                    // 无此属性，只能作罢
//                    ReflectLogHelper.logNoSuchField(log, startClass, property);
//                }
//                catch (SecurityException e)
//                {
//                    // 安全异常，这个，不解释
//                    ReflectLogHelper.logSecurity(log, e.getMessage());
//                }
//
//                startClass = startClass.getSuperclass();
//            }
//
//            if (null != field)
//            {
//                fieldCache.put(cacheKey, field);
//            }
//        }
//
//        return field;
//    }
    
    /**
     * 搜寻Field
     *
     * 搜寻指定类型中指定名称的数据域
     * 向上追溯直到Object类
     * 有属性覆盖的，先到先得
     *
     * @param property 被搜寻的域名
     * @return 实际找到的域，若找不到，则返回null
     */
    private Field searchField(final String property)
    {
        //String className = clazz.getName();
        //String cacheKey = className + "." + property;
        Field field = null;
        Class<?> startClass = clazz;
        while (null != startClass)
        {
            try
            {
                field = startClass.getDeclaredField(property);
                break;
            }
            catch (NoSuchFieldException e)
            {
                // 无此属性，只能作罢
                ReflectLogHelper.logNoSuchField(log, startClass, property);
            }
            catch (SecurityException e)
            {
                // 安全异常，这个，不解释
                ReflectLogHelper.logSecurity(log, e.getMessage());
            }

            startClass = startClass.getSuperclass();
        }

        return field;
    }

    /**
     * 精确搜寻Method
     *
     * 搜寻指定类型中指定名称的方法域
     * 向上追溯直到Object类
     * 有方法覆盖的，先到先得
     *
     * @param methodName 被搜寻的方法名
     * @param argTypes 参数类型列表
     * @return 实际找到的方法域，若找不到，则返回null
     */
//    @Deprecated
//    private Method searchMethod4Cache(final String methodName, final Class<?>... argTypes)
//    {
//        /*
//         * 创建方法缓存的KEY
//         * 规则为类名.方法名(逗号分割的参数类型列表)，以此来标记一个唯一的方法
//         */
//        StringBuilder cacheKeyBuilder = new StringBuilder(clazz.getName());
//        cacheKeyBuilder.append('.');
//        cacheKeyBuilder.append(methodName);
//        cacheKeyBuilder.append('(');
//        for (Class<?> argType : argTypes)
//        {
//            cacheKeyBuilder.append(argType.getName());
//            cacheKeyBuilder.append(',');
//        }
//        if (cacheKeyBuilder.charAt(cacheKeyBuilder.length() - 1) == ',')
//        {
//            cacheKeyBuilder.deleteCharAt(cacheKeyBuilder.length() - 1);
//        }
//        cacheKeyBuilder.append(')');
//        String cacheKey = cacheKeyBuilder.toString();
//
//        Method method = null;
//        if (methodCache.containsKey(cacheKey))
//        {
//            method = methodCache.get(cacheKey);
//        }
//        else
//        {
//            Class<?> startClass = clazz;
//            while (null != startClass)
//            {
//                try
//                {
//                    method = startClass.getDeclaredMethod(methodName, argTypes);
//                    break;
//                }
//                catch (NoSuchMethodException e)
//                {
//                    // 无此方法，只能作罢
//                    ReflectLogHelper.logNoSuchMethod(log, startClass, methodName, argTypes);
//                }
//                catch (SecurityException e)
//                {
//                    // 安全异常，这个，不解释
//                    ReflectLogHelper.logSecurity(log, e.getMessage());
//                }
//
//                startClass = startClass.getSuperclass();
//            }
//
//            if (null != method)
//            {
//                methodCache.put(cacheKey, method);
//            }
//        }
//
//        return method;
//    }
    
    /**
     * 精确搜寻Method
     *
     * 搜寻指定类型中指定名称的方法域
     * 向上追溯直到Object类
     * 有方法覆盖的，先到先得
     *
     * @param methodName 被搜寻的方法名
     * @param argTypes 参数类型列表
     * @return 实际找到的方法域，若找不到，则返回null
     */
    private Method searchMethod(final String methodName, final Class<?>... argTypes)
    {
        /*
         * 创建方法缓存的KEY
         * 规则为类名.方法名(逗号分割的参数类型列表)，以此来标记一个唯一的方法
         */
//        StringBuilder cacheKeyBuilder = new StringBuilder(clazz.getName());
//        cacheKeyBuilder.append('.');
//        cacheKeyBuilder.append(methodName);
//        cacheKeyBuilder.append('(');
//        for (Class<?> argType : argTypes)
//        {
//            cacheKeyBuilder.append(argType.getName());
//            cacheKeyBuilder.append(',');
//        }
//        if (cacheKeyBuilder.charAt(cacheKeyBuilder.length() - 1) == ',')
//        {
//            cacheKeyBuilder.deleteCharAt(cacheKeyBuilder.length() - 1);
//        }
//        cacheKeyBuilder.append(')');
        //String cacheKey = cacheKeyBuilder.toString();

        Method method = null;
        Class<?> startClass = clazz;
        while (null != startClass)
        {
            try
            {
                method = startClass.getDeclaredMethod(methodName, argTypes);
                break;
            }
            catch (NoSuchMethodException e)
            {
                // 无此方法，只能作罢
                ReflectLogHelper.logNoSuchMethod(log, startClass, methodName, argTypes);
            }
            catch (SecurityException e)
            {
                // 安全异常，这个，不解释
                ReflectLogHelper.logSecurity(log, e.getMessage());
            }

            startClass = startClass.getSuperclass();
        }

        return method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable
    {
        //fieldCache.clear();
        //methodCache.clear();
        //fieldCache = null;
        //methodCache = null;
        target = null;
        clazz = null;
        super.finalize();
    }

}
