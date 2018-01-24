package com.bms.util.methodreflect;

import java.util.HashMap;

/***
 * 
 * @Description: 不同数据库分页的具体实现方式的抽象类
 * @author 黄国强
 * @date 2017年5月10日
 *
 */
public abstract class Dialect
{
    /* 方言列表 */
    private static final HashMap<String, Dialect> dialects;

    static
    {
        dialects = new HashMap<String, Dialect>();
        dialects.put("MYSQL", new MySql5Dialect());
    }

    public static Dialect getDialect(final String dialectName)
    {
        return dialects.get(dialectName.toUpperCase());
    }

    /**
     * 获取带分页标识的SQL语句
     * @param sql 截取到的SQL语句
     * @param offset 偏移量
     * @param limit 记录条数
     * @return 修改后的SQL语句
     */
    public abstract String getLimitString(String sql, long offset, int limit);

    /**
     * 获取带某一SQL语句的计算总数的SQL语句
     * @param sql 原SQL语句
     * @return 带Count的SQL语句
     */
    public abstract String getCountString(String sql);

}
