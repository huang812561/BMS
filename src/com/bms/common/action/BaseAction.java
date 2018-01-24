package com.bms.common.action;

import java.util.Map;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class BaseAction extends ActionSupport {

	/**
	 * @Fields serialVersionUID :序列化使用的唯一标识
	 */
	private static final long serialVersionUID = -3669176870770326686L;


	public Map<String, Object> getSession() {
		return ActionContext.getContext().getSession();
	}

	public Map<String, Object> getContextMap() {
		return ActionContext.getContext().getContextMap();
	}

	public Map<String, Object> getParamMap() {
		return ActionContext.getContext().getParameters();
	}

	public ActionContext getContext() {
		return ActionContext.getContext();
	}

	public void putInSession(String key, Object value) {
		this.getSession().put(key, value);
	}

	public void putInContext(String key, Object value) {
		this.getContextMap().put(key, value);
	}

}
