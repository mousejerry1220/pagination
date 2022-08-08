package com.qmakesoft.framework.common.pagination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Jerry
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryCondition {

	FormatType formatType() default FormatType.empty;
	
	QueryType queryType() default QueryType.equals;
	/**
	 * @return like查询时后置通配符
	 */
	String append() default "";

	/**
	 * @return like查询时前置通配符
	 */
	String prepend() default "";

	/**
	 * @return 对应数据库的字段
	 */
	String[] fieldName() default "";
	
	/**
	 * @return MybatisGenerator生成的实体对象类型
	 */
	Class<?>[] fieldClass() default {};
	
	boolean searchNull() default false;
	/**
	 * 生成查询条件时的先后顺序
	 * @return 顺序大小
	 */
	int sn() default 10;
	
}
