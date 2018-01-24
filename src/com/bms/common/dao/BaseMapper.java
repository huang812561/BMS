package com.bms.common.dao;

import java.util.List;

/**
 * 抽象DAO基类
 * @author li_zhiyong
 *
 * @param <BaseEntity>
 */
public interface BaseMapper<T> {

	/** 保存 */
	public int save(T obj) throws Exception;
	
	/** 修改 */
	public  int update(T boj) throws Exception;
	
	/** 删除 */
	public  int delete(T obj) throws Exception;
	
	/** 查询单条记录 */
	public T queryOne(T obj) throws Exception;
	
	/** 查询多条条记录 */
	public List<T> queryList(T obj) throws Exception;
	
}
