package com.qmakesoft.framework.common.pagination;

import java.io.Serializable;

import org.springframework.util.StringUtils;

/**
 * 分页查询对象
 * @author Jerry
 *
 */
public abstract class PageCondition implements Serializable{

	public static final String ORDER_TYPE_DESCENDING = "descending";
	
	public static final String ORDER_TYPE_ASCENDING = "ascending";
	
	private static final long serialVersionUID = 1L;

	protected Integer page = 1;
	
	protected Integer pageSize = 10;
	
	protected String orderProp;
	
	protected String orderType;
	
	/**
	 * 根据查询条件orderProp与orderType生成排序字符串
	 * @return 排序字符串
	 */
	public String getOrderBy() {
		if(!StringUtils.hasLength(orderProp)) {
			return null;
		}
		validateField(orderProp);
		StringBuffer sb = new StringBuffer(getTableField(orderProp));
		if(ORDER_TYPE_DESCENDING.equals(orderType)) {
			sb.append(" desc");
		}
		return sb.toString();
	}
	
	public abstract Class<?> getEntityClass();
	
	private void validateField(String orderProp) {
		Class<?> cls = getEntityClass();
		if(cls == null) {
			return ;
		}
		try {
			cls.getDeclaredField(orderProp);
		} catch (NoSuchFieldException | SecurityException e) {
			if(cls.getSuperclass() == null) {
				throw new RuntimeException("排序字段异常，不存在改字段");
			}
			try {
				cls.getSuperclass().getDeclaredField(orderProp);
			} catch (NoSuchFieldException | SecurityException e1) {
				throw new RuntimeException("排序字段异常，不存在改字段");
			}
		}
	}

	/**
	 * 为了防止用户非法输入造成SQL注入，需要实现类覆盖该方法，返回安全的字段名称
	 * @param orderProp
	 * @return
	 */

	private final static String[] CHARS = new String[] {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
	
	protected String getTableField(String orderProp) {
		if(!StringUtils.hasLength(orderProp)) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<orderProp.length();i++) {
			char c = orderProp.charAt(i);
			if(isUpperCase(c)) {
				sb.append("_");
			}
			sb.append(c);
		}
		return sb.toString().toLowerCase();
	}

	private boolean isUpperCase(char c){
		for(String s : CHARS) {
			if(s.equals(String.valueOf(c))) {
				return true;
			}
		}		
		return false;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getOrderProp() {
		return orderProp;
	}

	public void setOrderProp(String orderProp) {
		this.orderProp = orderProp;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}
	
}
