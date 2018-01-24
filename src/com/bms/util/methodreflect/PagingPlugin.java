package com.bms.util.methodreflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.log4j.Logger;

import com.bms.util.commmon.SqlUtil;
import com.bms.util.commmon.StringUtil;
import com.ykc.api.comom.ConstDefine;

/**
 * Mybatis的分页查询插件，通过拦截Executor的query方法来实现。
 * 只有在参数列表中包括Page类型的参数时才进行分页查询。
 * 在多参数的情况下，只对第一个Page类型的参数生效。
 * 另外，在参数列表中，Page类型的参数无需用@Param来标注
 *
*/
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PagingPlugin extends PluginBase {

    /* 日志 */
    private static final Logger log = Logger.getLogger(PagingPlugin.class);


    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object result = null;
        Executor executor = (Executor) unProxy(invocation.getTarget());
        executor = unDelegate(executor);
        boolean isExecuted = false;
        // 拿参数
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        if(args[1] instanceof Object){
        	Map<String,?> objMap = (Map<String,?>)Obj2Map(args[1]);
	        if (objMap instanceof Map <?, ?>) {
	            // 获取参数并判定是否需要分页
	            Map <String, String> paramsMap = (Map <String, String>)objMap;
	            if(paramsMap.containsKey(ConstDefine.PAGE_SORT_PARAM.ORDER_NAME) || paramsMap.containsKey(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX)) {
	                //只有符合这个条件才需要进行和分页相关的操作
	                // 获取列名映射
	                Map<String, String> columnMap = getColumnMap(mappedStatement);
	
	                // 规整SQL文
	                synchronized (mappedStatement) {
	                    final BoundSql boundSql = mappedStatement.getBoundSql(objMap);
	                    synchronized (boundSql) {
	                        String nativeSql = boundSql.getSql();
	                        String formatedSql = null;
	                        String pagedSql = null;
	                        if (paramsMap.containsKey(ConstDefine.PAGE_SORT_PARAM.ORDER_NAME)) {
	                            formatedSql = formatSql(nativeSql, paramsMap, columnMap);
	                        } else {
	                            formatedSql = nativeSql;
	                        }
	
	                        // 判定分页
	                        /*if (paramsMap.containsKey(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX)) {
	                            pagedSql = doPaging(paramsMap, mappedStatement, formatedSql, executor, boundSql);
	                        } else {
	                            pagedSql = formatedSql;
	                        }*/
	                        // 参数伪装：SQL
	                        MetaObject<BoundSql> boundSqlMeta = MetaObject.fromObject(boundSql);
	                        boundSqlMeta.setValue("sql", formatedSql);
	
	                        // 参数伪装：SqlSource
	                        MetaObject<MappedStatement> mappedStatementMeta = MetaObject.fromObject(mappedStatement);
	                        final SqlSource sqlSource = (SqlSource) mappedStatementMeta.getValue("sqlSource");
	                        SqlSource proxyedSqlSource = (SqlSource) Proxy.newProxyInstance(sqlSource.getClass().getClassLoader(), sqlSource.getClass()
	                                .getInterfaces(), new InvocationHandler() {
	
	                            @Override
	                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
	                                Object result = method.invoke(sqlSource, args);
	
	                                if ("getBoundSql".equals(method.getName())) {
	                                    result = boundSql;
	                                }
	
	                                return result;
	                            }
	                        });
	                        mappedStatementMeta.setValue("sqlSource", proxyedSqlSource);
	
	                        // 执行
	                        try {
	                            result = invocation.proceed();
	                            isExecuted = true;
	                        } catch (Exception e) {
	                            log.error("SQL执行错误："+pagedSql);
	                            throw e;
	                        } finally {
	                            // 参数恢复：SqlSource
	                            mappedStatementMeta.setValue("sqlSource", sqlSource);
	                            boundSqlMeta.setValue("sql", nativeSql); // 不知道有没有用哈
	                        }
	                    }
	                }
	            }
	        }
        }
        if (!isExecuted) {
            result = invocation.proceed();
        }

        
        return result;
    }

    public static Map<String,Object> Obj2Map(Object obj) throws Exception{
        Map<String,Object> map=new HashMap<String, Object>();
        Class<?> clazz = obj.getClass();
        for(; clazz != Object.class;clazz = clazz.getSuperclass()){
	        Field[] fields = clazz.getDeclaredFields();
	        for(Field field:fields){
	            field.setAccessible(true);
	            map.put(field.getName(), field.get(obj));
	        }
        }
        return map;
    }

    private String doPaging(Map <String, String> paramsMap, MappedStatement mappedStatement, String formatedSql, Executor executor,
            final BoundSql boundSql) {

        String pagedSql = formatedSql;
        if (paramsMap.containsKey(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX)) {
            Dialect dialect = guessDialect(mappedStatement);
            String countSql = dialect.getCountString(formatedSql);

            long total = calcCount(executor, mappedStatement, paramsMap, boundSql, countSql);

            paramsMap.put(ConstDefine.PAGE_SORT_PARAM.TOTAL_COUNT, String.valueOf(total));

            if (total > 0) {
                String pageNoStr = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX);
                String pageSizeStr = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.PAGE_SIZE);

                /*
                 * 处理分页参数，如果参数不合法，将会以默认值替代,不影响功能继续执行
                 */
                int pageNo = parsePageNo(pageNoStr);
                int pageSize = parsePageSize(pageSizeStr);

                // 计算物理分页参数,并纠正
                long offset = calcOffset(ConstDefine.NUMBER.NUMBER_0, pageNo, pageSize);

                // 获取数据库方言类来处理物理分页
                pagedSql = dialect.getLimitString(formatedSql, offset, pageSize);

                if (log.isDebugEnabled()) {
                    log.debug("分页处理后:{}"+ pagedSql);
                }
            }
        }

        return pagedSql;
    }


    private long calcCount(Executor executor, MappedStatement mappedStatement, Object parameters, BoundSql boundSql, String countSql) {

        MetaObject <BoundSql> boundSqlMeta = MetaObject.fromObject(boundSql);
        boundSqlMeta.setValue("sql", countSql);
        RowBounds rowBounds = new RowBounds(0, 1);

        long count = 0;
        CacheKey cacheKey = executor.createCacheKey(mappedStatement, parameters, rowBounds, boundSql);
        MetaObject <MappedStatement> mappedStatementMeta = MetaObject.fromObject(mappedStatement);
        Configuration config = unDelegate(mappedStatement.getConfiguration());
        try {
            // 植入假配置
            Configuration pageConfig = new PageConfiguration(config);
            mappedStatementMeta.setValue("configuration", pageConfig);
            List <Object> result = executor.query(mappedStatement, parameters, rowBounds, new DefaultResultHandler(), cacheKey, boundSql);
            Iterator <Object> it = result.iterator();
            if (it.hasNext()) {
                count = (Long) it.next();
            }
        } catch (SQLException e) {
                log.error("计算总数时发生异常!!!", e);
        } finally {
            // 还原真配置
            mappedStatementMeta.setValue("configuration", config);
        }

        return count;
    }


    /*
     * 计算偏移量
     *
     * @param total 总记录数
     * @param pageNo 页号
     * @param pageSize 页容
     * @return 偏移量
     */
    private long calcOffset(long total, int pageNo, int pageSize) {
/*
        int totalPage = (int) (total / pageSize);
        if (total % pageSize != 0) {
            totalPage++;
        }

        if (pageNo > totalPage) {
            pageNo = totalPage;
            StringBuilder logInfo = new StringBuilder("页面号过大：");
            logInfo.append(pageNo);
            logInfo.append("，将自动计算为最后一页。");
            log.warn(logInfo.toString());
        }*/

        long offset = (pageNo - 1) * (long) pageSize;

        return offset;
    }


    private int parsePageNo(String pageNoStr) {

        int pageNo = ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_IDX;

        try {
            pageNo = Integer.parseInt(pageNoStr);
        } catch (NumberFormatException e) {
                String tips = "该参数值必须能解析为数字!";
                String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX, pageNoStr, ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_IDX, tips);
                log.warn(logInfo.toString());
        }

        if (pageNo < 1) {
                String tips = "该参数值不能小于1!";
                String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.PAGE_IDX, pageNoStr, ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_IDX, tips);
                log.warn(logInfo.toString());
            pageNo = ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_IDX;
        }

        return pageNo;
    }


    private int parsePageSize(String pageSizeStr) {

        int pageSize = ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_SIZE;

        try {
            pageSize = Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException e) {
                String tips = "该参数值必须能解析为数字!";
                String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.PAGE_SIZE, pageSizeStr, ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_SIZE, tips);
                log.warn(logInfo.toString());
        }

        if (pageSize < 1) {
            String tips = "该参数值不能小于1!";
            String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.PAGE_SIZE, pageSizeStr, ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_SIZE, tips);
            log.warn(logInfo.toString());
            pageSize = ConstDefine.PAGE_SORT_PARAM.DEFAULT_PAGE_SIZE;
        }

        return pageSize;
    }


    private String formatSql(String nativeSql, Map <String, String> paramsMap, Map <String, String> columnMap) {

        // 获取查询sql语句后对其进行清理
        String cleanedSql = SqlUtil.cleanSql(nativeSql);

            log.debug("处理前SQL:{}"+ nativeSql);
            log.debug("清理后SQL:{}"+ cleanedSql);

        // 如果sql中有like操作，就对所有的like后追加ESCAPE逃逸字符
        String regex = "\\s+[Ll][Ii][Kk][Ee]\\s+[?]\\s*";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cleanedSql);
        cleanedSql = matcher.replaceAll(" LIKE ? ESCAPE '/' ");
        /*
         * 检测最外层SQL中是否存在排序语句
         */
        boolean hasOrder = false;
        int lastRBracketIdx = cleanedSql.lastIndexOf(')');
        if (cleanedSql.toUpperCase().indexOf("ORDER BY", lastRBracketIdx) > 0) {
            hasOrder = true;
        }
        
        /*
         * 检测最外层SQL中是否存在分页语句
         */
        boolean hasLimit = false;
        String limitStr = "";
        if (cleanedSql.toUpperCase().indexOf("LIMIT") > 0) {
        	hasLimit = true;
        	int start = cleanedSql.toUpperCase().indexOf("LIMIT");
        	limitStr = cleanedSql.substring(start, cleanedSql.length());
        	cleanedSql = cleanedSql.substring(0, start);
        	
        }

        /*
         *处理排序的字段名和排序类型
         *如果字段名不存在，在用默认的更新时间逆序来排序
         *字段存在，但排序类型参数值如果不合法，则默认按照顺序排列
         */
        StringBuilder sqlBuilder = new StringBuilder(cleanedSql);
        String orderName = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.ORDER_NAME);
        String orderType = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE);
        String intOrderColumn = paramsMap.get("page.intOrderColumn");
        if (intOrderColumn != null) {
            intOrderColumn = intOrderColumn.trim().toUpperCase();
        }

        if (null != orderName) {
            String orderColumn = columnMap.get(orderName);
            if (null == orderColumn) {
                String tips = "该参数应当为实体中存在的属性名!";
                String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.ORDER_NAME, orderName, "UPD_TIME", tips);
                log.warn(logInfo);
            } else {
                if (!hasOrder) {
                    sqlBuilder.append(" ORDER BY ");
                } else {
                    sqlBuilder.append(",");
                }

                // 拼排序字段  edit by wu_dh  2013-04-17
                String curOrderColumn = orderColumn.toUpperCase() + ",";
                if (intOrderColumn != null && intOrderColumn.indexOf(curOrderColumn) >= 0) {
                    // Modified by wang_chenggong on 2013/11/28.
                    //sqlBuilder.append(" CONVERT(BIGINT,").append(orderColumn).append(") ");
                    sqlBuilder.append(" CAST(").append(orderColumn).append(" AS BIGINT) ");
                } else {
                    sqlBuilder.append(orderColumn);
                }

                // 拼排序类型
                if (null == orderType || !("DESC".equalsIgnoreCase(orderType.trim()) || "ASC".equalsIgnoreCase(orderType.trim()))) {
                    orderType = ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE_ASC;

                    String tips = "该参数支持\"ASC\"|\"DESC\"大小写不敏感";
                    String logInfo = mkInvldPrmLg(ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE, orderType, ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE_ASC, tips);
                    log.warn(logInfo.toString());
                }

                sqlBuilder.append(" ");
                sqlBuilder.append(orderType.toUpperCase());

                // 处理辅助排序
                String asistOrderName = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.ASIST_ORDER_NAME);

                if (StringUtil.isNotEmpty(asistOrderName) && !asistOrderName.equals(columnMap.get(orderName))) {
                    /*
                     * 辅助排序的值应当为表中实际存在的列名
                     * 插件中不负责对列明的校验
                     * 该参数的设定用于解决当按照某一列排序时，因此列中数据重复较多而导致分页混乱的问题
                     * 不到万不得已，不要使用这个终极方法，切记
                     */
                    sqlBuilder.append(",");
                    sqlBuilder.append(asistOrderName);
                    String asistOrderType = paramsMap.get(ConstDefine.PAGE_SORT_PARAM.ASIST_ORDER_TYPE);
                    if (StringUtil.isNotEmpty(asistOrderType)) {
                        if (asistOrderType.equalsIgnoreCase(ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE_DESC)) {
                            /*
                             * 当且仅当辅助排序类型参数被设定成DESC(无视大小写)时，会按照逆序排
                             * 其他任何情况都不追加排序类型
                             */
                            sqlBuilder.append(" ");
                            sqlBuilder.append(ConstDefine.PAGE_SORT_PARAM.ORDER_TYPE_DESC);
                        }
                    }
                }
            }
        }

        if(hasLimit){
        	sqlBuilder.append(" " + limitStr);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("排序处理后:{}"+ sqlBuilder);
        }

        return sqlBuilder.toString();
    }


    /*
     * 先处理排序问题
     * 获取当前结果集属性-列名映射
     * 用作排序时作转换用
     */
    private Map <String, String> getColumnMap(MappedStatement mappedStatement) {

        Map <String, String> columnMap = new HashMap <String, String>();
        List <ResultMap> resultMaps = mappedStatement.getResultMaps();
        for (ResultMap resultMap : resultMaps) {
            List <ResultMapping> resultMappings = resultMap.getResultMappings();
            for (ResultMapping resultMapping : resultMappings) {
                String property = resultMapping.getProperty();
                String column = resultMapping.getColumn();
                columnMap.put(property, column);
            }
        }
        return columnMap;
    }


    private Dialect guessDialect(MappedStatement mappedStatement) {

        Configuration configuration = mappedStatement.getConfiguration();
        String dialectName = configuration.getVariables().getProperty("dialect");
        Dialect dialect = Dialect.getDialect(dialectName);

        return dialect;
    }


    /*
     * 构建因参数非法而产生的日志信息
     *
     * @param param 参数名
     * @param paramValue 传入值
     * @param defaultValue 替补值
     * @param tips 额外提示
     *
     * @author 葛永山
     */
    private static String mkInvldPrmLg(String param, Object paramValue, Object defaultValue, String tips) {

        StringBuilder logInfo = new StringBuilder("非法的\"");
        logInfo.append(param);
        logInfo.append("\"值:");
        logInfo.append(paramValue);
        logInfo.append("被传入，将使用默认值:");
        logInfo.append(defaultValue);
        logInfo.append("替代。");
        logInfo.append(tips);

        return logInfo.toString();
    }
}


class PageConfiguration extends Configuration {

    private final Configuration delegate;


    public PageConfiguration(Configuration delegate) {

        super();
        this.delegate = delegate;
    }


    @Override
    public void addCache(Cache cache) {

        delegate.addCache(cache);
    }


    @Override
    public void addCacheRef(String namespace, String referencedNamespace) {

        delegate.addCacheRef(namespace, referencedNamespace);
    }


    @Override
    public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {

        delegate.addIncompleteCacheRef(incompleteCacheRef);
    }


    @Override
    public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {

        delegate.addIncompleteResultMap(resultMapResolver);
    }


    @Override
    public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {

        delegate.addIncompleteStatement(incompleteStatement);
    }


    @Override
    public void addInterceptor(Interceptor interceptor) {

        delegate.addInterceptor(interceptor);
    }


    @Override
    public void addKeyGenerator(String id, KeyGenerator keyGenerator) {

        delegate.addKeyGenerator(id, keyGenerator);
    }


    @Override
    public void addLoadedResource(String resource) {

        delegate.addLoadedResource(resource);
    }


    @Override
    public void addMappedStatement(MappedStatement ms) {

        delegate.addMappedStatement(ms);
    }


    @Override
    public <T> void addMapper(Class <T> type) {

        delegate.addMapper(type);
    }


    @Override
    public void addMappers(String packageName, Class <?> delegateType) {

        delegate.addMappers(packageName, delegateType);
    }


    @Override
    public void addMappers(String packageName) {

        delegate.addMappers(packageName);
    }


    @Override
    public void addParameterMap(ParameterMap pm) {

        delegate.addParameterMap(pm);
    }


    @Override
    public void addResultMap(ResultMap rm) {

        delegate.addResultMap(rm);
    }


    @Override
    protected void buildAllStatements() {

        try {
            Method method = delegate.getClass().getMethod("buildAllStatements");
            method.setAccessible(true);
            method.invoke(delegate);
        } catch (SecurityException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalArgumentException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (NoSuchMethodException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalAccessException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (InvocationTargetException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        }
    }


    @Override
    protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {

        try {
            Method method = delegate.getClass().getMethod("checkGloballyForDiscriminatedNestedResultMaps", ResultMap.class);
            method.setAccessible(true);
            method.invoke(delegate, rm);
        } catch (SecurityException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalArgumentException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (NoSuchMethodException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalAccessException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (InvocationTargetException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        }
    }


    @Override
    protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {

        try {
            Method method = delegate.getClass().getMethod("checkLocallyForDiscriminatedNestedResultMaps", ResultMap.class);
            method.setAccessible(true);
            method.invoke(delegate, rm);
        } catch (SecurityException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalArgumentException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (NoSuchMethodException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalAccessException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (InvocationTargetException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        }
    }


    @Override
    protected String extractNamespace(String statementId) {

        try {
            Method method = delegate.getClass().getMethod("extractNamespace", String.class);
            method.setAccessible(true);
            return (String) method.invoke(delegate, statementId);
        } catch (SecurityException e) {
            Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalArgumentException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (NoSuchMethodException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (IllegalAccessException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        } catch (InvocationTargetException e) {
        	Logger logger = Logger.getLogger(this.getClass());
            logger.error("sonar操作错误：" + e.getMessage(),e);
        }

        return null;
    }


    @Override
    public AutoMappingBehavior getAutoMappingBehavior() {

        return delegate.getAutoMappingBehavior();
    }


    @Override
    public Cache getCache(String id) {

        return delegate.getCache(id);
    }


    @Override
    public Collection <String> getCacheNames() {

        return delegate.getCacheNames();
    }


    @Override
    public Collection <Cache> getCaches() {

        return delegate.getCaches();
    }


    @Override
    public String getDatabaseId() {

        return delegate.getDatabaseId();
    }


    @Override
    public ExecutorType getDefaultExecutorType() {

        return delegate.getDefaultExecutorType();
    }


    @Override
    public Integer getDefaultStatementTimeout() {

        return delegate.getDefaultStatementTimeout();
    }


    @Override
    public Environment getEnvironment() {

        return delegate.getEnvironment();
    }


    @Override
    public Collection <CacheRefResolver> getIncompleteCacheRefs() {

        return delegate.getIncompleteCacheRefs();
    }


    @Override
    public Collection <ResultMapResolver> getIncompleteResultMaps() {

        return delegate.getIncompleteResultMaps();
    }


    @Override
    public Collection <XMLStatementBuilder> getIncompleteStatements() {

        return delegate.getIncompleteStatements();
    }


    @Override
    public JdbcType getJdbcTypeForNull() {

        return delegate.getJdbcTypeForNull();
    }


    @Override
    public KeyGenerator getKeyGenerator(String id) {

        return delegate.getKeyGenerator(id);
    }


    @Override
    public Collection <String> getKeyGeneratorNames() {

        return delegate.getKeyGeneratorNames();
    }


    @Override
    public Collection <KeyGenerator> getKeyGenerators() {

        return delegate.getKeyGenerators();
    }


    @Override
    public Set <String> getLazyLoadTriggerMethods() {

        return delegate.getLazyLoadTriggerMethods();
    }


    @Override
    public LocalCacheScope getLocalCacheScope() {

        return delegate.getLocalCacheScope();
    }


    @Override
    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {

        return delegate.getMappedStatement(id, validateIncompleteStatements);
    }


    @Override
    public MappedStatement getMappedStatement(String id) {

        return delegate.getMappedStatement(id);
    }


    @Override
    public Collection <String> getMappedStatementNames() {

        return delegate.getMappedStatementNames();
    }


    @Override
    public Collection <MappedStatement> getMappedStatements() {

        return delegate.getMappedStatements();
    }


    @Override
    public <T> T getMapper(Class <T> type, SqlSession sqlSession) {

        return delegate.getMapper(type, sqlSession);
    }


    @Override
    public ObjectFactory getObjectFactory() {

        return delegate.getObjectFactory();
    }


    @Override
    public ObjectWrapperFactory getObjectWrapperFactory() {

        return delegate.getObjectWrapperFactory();
    }


    @Override
    public ParameterMap getParameterMap(String id) {

        return delegate.getParameterMap(id);
    }


    @Override
    public Collection <String> getParameterMapNames() {

        return delegate.getParameterMapNames();
    }


    @Override
    public Collection <ParameterMap> getParameterMaps() {

        return delegate.getParameterMaps();
    }


    @Override
    public ResultMap getResultMap(String id) {

        return delegate.getResultMap(id);
    }


    @Override
    public Collection <String> getResultMapNames() {

        return delegate.getResultMapNames();
    }


    @Override
    public Collection <ResultMap> getResultMaps() {

        return delegate.getResultMaps();
    }


    @Override
    public Map <String, XNode> getSqlFragments() {

        return delegate.getSqlFragments();
    }


    @Override
    public TypeAliasRegistry getTypeAliasRegistry() {

        return delegate.getTypeAliasRegistry();
    }


    @Override
    public TypeHandlerRegistry getTypeHandlerRegistry() {

        return delegate.getTypeHandlerRegistry();
    }


    @Override
    public Properties getVariables() {

        return delegate.getVariables();
    }


    @Override
    public boolean hasCache(String id) {

        return delegate.hasCache(id);
    }


    @Override
    public boolean hasKeyGenerator(String id) {

        return delegate.hasKeyGenerator(id);
    }


    @Override
    public boolean hasMapper(Class <?> type) {

        return delegate.hasMapper(type);
    }


    @Override
    public boolean hasParameterMap(String id) {

        return delegate.hasParameterMap(id);
    }


    @Override
    public boolean hasResultMap(String id) {

        return delegate.hasResultMap(id);
    }


    @Override
    public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {

        return delegate.hasStatement(statementName, validateIncompleteStatements);
    }


    @Override
    public boolean hasStatement(String statementName) {

        return delegate.hasStatement(statementName);
    }


    @Override
    public boolean isAggressiveLazyLoading() {

        return delegate.isAggressiveLazyLoading();
    }


    @Override
    public boolean isCacheEnabled() {

        return delegate.isCacheEnabled();
    }


    @Override
    public boolean isLazyLoadingEnabled() {

        return delegate.isLazyLoadingEnabled();
    }


    @Override
    public boolean isMapUnderscoreToCamelCase() {

        return delegate.isMapUnderscoreToCamelCase();
    }


    @Override
    public boolean isMultipleResultSetsEnabled() {

        return delegate.isMultipleResultSetsEnabled();
    }


    @Override
    public boolean isUseColumnLabel() {

        return delegate.isUseColumnLabel();
    }


    @Override
    public boolean isResourceLoaded(String resource) {

        return delegate.isResourceLoaded(resource);
    }


    @Override
    public boolean isSafeResultHandlerEnabled() {

        return delegate.isSafeResultHandlerEnabled();
    }


    @Override
    public boolean isSafeRowBoundsEnabled() {

        return delegate.isSafeRowBoundsEnabled();
    }


    @Override
    public boolean isUseGeneratedKeys() {

        return delegate.isUseGeneratedKeys();
    }


    @Override
    public Executor newExecutor(Transaction transaction, ExecutorType executorType, boolean autoCommit) {

        return delegate.newExecutor(transaction, executorType, autoCommit);
    }


    @Override
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {

        return delegate.newExecutor(transaction, executorType);
    }


    @Override
    public Executor newExecutor(Transaction transaction) {

        return delegate.newExecutor(transaction);
    }


    @Override
    public org.apache.ibatis.reflection.MetaObject newMetaObject(Object object) {

        return delegate.newMetaObject(object);
    }


    @Override
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {

        return delegate.newParameterHandler(mappedStatement, parameterObject, boundSql);
    }


    @Override
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
            ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {

        return new ResultSetHandler() {

            private Logger logger = Logger.getLogger(this.getClass());
            @Override
            public void handleOutputParameters(CallableStatement cs) throws SQLException {

            }


            @SuppressWarnings("unchecked")
            @Override
            public List <Long> handleResultSets(Statement stmt) throws SQLException {

                List <Long> count = new ArrayList <Long>();
                ResultSet rs = stmt.getResultSet();
                try {
                    if (rs.next()) {
                        count.add(rs.getLong(1));
                    }
                }
                catch (Exception ex) {
                    logger.error("handleResultSets:操作错误" + ex.getMessage(),ex);
                }
                finally {
                    rs.close();
                }


                return count;
            }
        };
    }


    @Override
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds,
            ResultHandler resultHandler, BoundSql boundSql) {

        return delegate.newStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    }


    @Override
    public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {

        delegate.setAggressiveLazyLoading(aggressiveLazyLoading);
    }


    @Override
    public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {

        delegate.setAutoMappingBehavior(autoMappingBehavior);
    }


    @Override
    public void setCacheEnabled(boolean cacheEnabled) {

        delegate.setCacheEnabled(cacheEnabled);
    }


    @Override
    public void setDatabaseId(String databaseId) {

        delegate.setDatabaseId(databaseId);
    }


    @Override
    public void setDefaultExecutorType(ExecutorType defaultExecutorType) {

        delegate.setDefaultExecutorType(defaultExecutorType);
    }


    @Override
    public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {

        delegate.setDefaultStatementTimeout(defaultStatementTimeout);
    }


    @Override
    public void setEnvironment(Environment environment) {

        delegate.setEnvironment(environment);
    }


    @Override
    public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {

        delegate.setJdbcTypeForNull(jdbcTypeForNull);
    }


    @Override
    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {

        delegate.setLazyLoadingEnabled(lazyLoadingEnabled);
    }


    @Override
    public void setLazyLoadTriggerMethods(Set <String> lazyLoadTriggerMethods) {

        delegate.setLazyLoadTriggerMethods(lazyLoadTriggerMethods);
    }


    @Override
    public void setLocalCacheScope(LocalCacheScope localCacheScope) {

        delegate.setLocalCacheScope(localCacheScope);
    }


    @Override
    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {

        delegate.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
    }


    @Override
    public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {

        delegate.setMultipleResultSetsEnabled(multipleResultSetsEnabled);
    }


    @Override
    public void setObjectFactory(ObjectFactory objectFactory) {

        delegate.setObjectFactory(objectFactory);
    }


    @Override
    public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {

        delegate.setObjectWrapperFactory(objectWrapperFactory);
    }


    @Override
    public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {

        delegate.setSafeResultHandlerEnabled(safeResultHandlerEnabled);
    }


    @Override
    public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {

        delegate.setSafeRowBoundsEnabled(safeRowBoundsEnabled);
    }


    @Override
    public void setUseColumnLabel(boolean useColumnLabel) {

        delegate.setUseColumnLabel(useColumnLabel);
    }


    @Override
    public void setUseGeneratedKeys(boolean useGeneratedKeys) {

        delegate.setUseGeneratedKeys(useGeneratedKeys);
    }


    @Override
    public void setVariables(Properties variables) {

        delegate.setVariables(variables);
    }
}
