package com.bms.util.methodreflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.slf4j.Logger;

/**
 * 反射异常日志记录工具。
 * 
 */
final class ReflectLogHelper
{
    /**
     * private Constructor
     */
    private ReflectLogHelper()
    {
        super();
    }
    
    /**
     * 记录非法访问日志。
     * 
     * @param log 日志对象
     * @param method 被非法访问的方法
     */
    public static void logIllegalAccess(final Logger log, final Method method)
    {
        if (log.isInfoEnabled())
        {
            log.info("illegal access! method:{}", method);
        }
    }
    
    /**
     * 记录非法参数日志。
     * 
     * @param log 日志对象
     * @param method 访问的方法
     * @param argument 参数
     */
    public static void logIllegalArgument(final Logger log, final Method method, final Object argument)
    {
        if (log.isInfoEnabled())
        {
            log.info("illegal argument! method:{},argument:{}", method, argument);
        }
    }
    
    /**
     * 记录非法目标日志。
     * 
     * @param log 日志对象
     * @param method 访问的方法
     * @param target 被非法访问的目标
     */
    public static void logInvocationTarget(final Logger log, final Method method, final Object target)
    {
        if (log.isInfoEnabled())
        {
            log.info("illegal target! method:{},target:{}", method, target);
        }
    }
    
    /**
     * 记录无此属性异常。
     * 
     * @param log 日志对象
     * @param clazz 目标类对象
     * @param field 想获取的属性名
     */
    public static void logNoSuchField(final Logger log, final Class<?> clazz, final String field)
    {
        if (log.isInfoEnabled())
        {
            //修改了这里的日志级别，原来是info；现在改为debug；让日志文件中不至于显示如此多的内容
            log.debug("no such field! clazz:{},field:{}", clazz, field);
        }
    }
    
    /**
     * 记录无此方法异常。
     * 
     * @param log 日志对象
     * @param clazz 目标类对象
     * @param method 想获取的方法名
     * @param argTypes 参数类型数组
     */
    public static void logNoSuchMethod(final Logger log, final Class<?> clazz, final String method, final Class<?>... argTypes)
    {
        if (log.isInfoEnabled())
        {
            log.debug("no such method! clazz:{},method:{}({})", clazz, method, Arrays.toString(argTypes));
        }
    }
    
    /**
     * 记录安全异常日志。
     * 
     * @param log 日志对象
     * @param reason 缘由
     */
    public static void logSecurity(final Logger log, final String reason)
    {
        if (log.isInfoEnabled())
        {
            log.info("security problem! reason:{}", reason);
        }
    }

}

