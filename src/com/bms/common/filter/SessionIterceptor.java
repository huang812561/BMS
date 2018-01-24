package com.bms.common.filter;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class SessionIterceptor extends AbstractInterceptor {

	/**
	 * 
	 */ 
	private static final long serialVersionUID = 471730925547300873L;

	@Override
	public String intercept(ActionInvocation actionInvocation) throws Exception {
		
		
		return actionInvocation.invoke();
	}
	
	

}
