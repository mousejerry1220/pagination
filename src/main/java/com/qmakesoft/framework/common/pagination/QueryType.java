package com.qmakesoft.framework.common.pagination;

public enum QueryType {
	
	equals("EqualTo") , like("Like"), gt("GreaterThan") , lt("LessThan") , gte("GreaterThanOrEqualTo"), lte("LessThanOrEqualTo");
	
	public String method;
	
	QueryType(String method){
		this.method = method;
	}

	public String getMethodName(String fieldName) {
		StringBuffer methodName = new StringBuffer(); 
		methodName.append("and").append(fieldName).append(method);
		return methodName.toString();
	};
	
	public String getSearchNullMethodName(String fieldName) {
		StringBuffer methodName = new StringBuffer(); 
		methodName.append("and").append(fieldName).append("IsNull");
		return methodName.toString();
	};
	
}
