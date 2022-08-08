package com.qmakesoft.framework.common.pagination;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

public class PageUtils {
	
	public static <B> List<B> convert(List<?> alist,Class<B> cls) {
		List<B> bList = new Page<>();
		BeanUtils.copyProperties(alist, bList);
		for(Object a : alist) {
			B b = null;
			try {
				b = cls.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			BeanUtils.copyProperties(a, b);
			bList.add(b);
		}
		return bList;
	}
	
	public static <T> T conditionToExample(PageCondition pageCondition , Class<T> exampleClass ) {
		return conditionToExample(pageCondition, exampleClass, true);
	}
	
	public static <T> T conditionToExample(PageCondition pageCondition , Class<T> exampleClass , boolean isPage) {
		Class<?> pageConditionClass = pageCondition.getClass();
		Field[] fields = pageConditionClass.getDeclaredFields();
		T t = null;
		try {
			t = exampleClass.newInstance();
			
			//判断是否存在集合式条件
			Field collectionField = null;
			Collection<?> collection = null;
			QueryCondition collectionQueryCondition = null;
			String[] fieldNames = null;
			
			Field[] orderFieldList= null;
			
			for(Field field : fields) {
				QueryCondition queryCondition = field.getAnnotation(QueryCondition.class);
				if(queryCondition == null) {
					continue;
				}
				field.setAccessible(true);
				Object value = field.get(pageCondition);
				
				if(value instanceof String && !StringUtils.hasLength((String)value)) {
					continue;
				}
				
				if(value instanceof Collection) {
					collectionField = field;
					collection = (Collection<?>)value;
					collectionQueryCondition = queryCondition;
				}
				
				if(collection == null) {
					if(queryCondition.fieldName() != null && queryCondition.fieldName().length > 1) {
						collectionField = field;
						fieldNames = queryCondition.fieldName();
						collectionQueryCondition = queryCondition;
					}
				}
			}
			
			orderFieldList = orderFields(fields);
			
			if(collection != null && collection.size() > 0) {
				Method createCriteriaMethod = exampleClass.getMethod("or");
				for(Object value : collection) {
					Object criteria = createCriteriaMethod.invoke(t);
					if(collectionQueryCondition.fieldName() != null) {
						if(collectionQueryCondition.fieldName().length > 1) {
							throw new RuntimeException("集合条件不允许设置多个查询字段");
						}
					}
					String fieldName = ObjectUtils.isEmpty(collectionQueryCondition.fieldName()) ? collectionField.getName() : collectionQueryCondition.fieldName()[0];
					Class<?> cls = null;
					if(collectionQueryCondition.fieldClass() == null || collectionQueryCondition.fieldClass().length == 0) {
						Type[] actualTypes = ((java.lang.reflect.ParameterizedType)collectionField.getGenericType()).getActualTypeArguments();
						cls = (Class<?>)actualTypes[0];
					}else {
						cls = collectionQueryCondition.fieldClass()[0];
					}
					setCriteria(pageCondition, orderFieldList, criteria, collectionField , fieldName , value, cls);
				}
			}
			else if(fieldNames != null) {
				Method createCriteriaMethod = exampleClass.getMethod("or");
				for(String fieldName: fieldNames) {
					Object criteria = createCriteriaMethod.invoke(t);
					setCriteria(pageCondition, orderFieldList, criteria, collectionField, fieldName,null,null);
				}
			}
			else {
				Method createCriteriaMethod = exampleClass.getMethod("createCriteria");
				Object criteria = createCriteriaMethod.invoke(t);
				setCriteria(pageCondition, orderFieldList, criteria , null , null , null, null);
			}
			
			
			//设置排序
			if(StringUtils.hasLength(pageCondition.getOrderBy())) {
				Method orderByMethod = exampleClass.getMethod("setOrderByClause", String.class);
				orderByMethod.invoke(t, pageCondition.getOrderBy());
			}
			
			//设置分页
			if(isPage) {
				PageHelper.startPage(pageCondition.getPage(), pageCondition.getPageSize());
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return t;
	}
	
	private static Field[] orderFields(Field[] fields) {
		
		List<Field> newFieldList = new ArrayList<>(); 
		
		for(int i=0;i<fields.length ;i++) {
			Field fieldA = fields[i];
			QueryCondition queryConditionA = fieldA.getAnnotation(QueryCondition.class);
			if(queryConditionA != null) {
				newFieldList.add(fieldA);
			}
		}
		
		Field[] newFields = new Field[newFieldList.size()];
		for(int i=0;i<newFieldList.size();i++) {
			newFields[i] = newFieldList.get(i);
		}
		
		fields = newFields;
		for(int i=0;i<fields.length ;i++) {
			if(fields[i] == null) continue;
			for(int j=i+1;j<fields.length ;j++) {
				if(fields[j] == null) continue;
				Field fieldA = fields[i];
				Field fieldB = fields[j];
				QueryCondition queryConditionA = fieldA.getAnnotation(QueryCondition.class);
				QueryCondition queryConditionB = fieldB.getAnnotation(QueryCondition.class);
				if(queryConditionA.sn() > queryConditionB.sn()) {
					fields[i] = fieldB;
					fields[j] = fieldA;
				}
			}
		}
		return fields;
	}

	private static void setCriteria(PageCondition pageCondition, Field[] fields, Object criteria, Field collectionField ,String collectionFieldName ,Object collectionValue ,Class<?> cls)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		//设置查询字段
		for(Field field : fields) {
			QueryCondition queryCondition = field.getAnnotation(QueryCondition.class);
			if(queryCondition == null) {
				continue;
			}
			field.setAccessible(true);
			
			Object value = field.get(pageCondition);
			if(collectionValue != null) {
				value = collectionValue;
			}
			
			if(value == null) {
				//如果需要查询为null的情况
				if(queryCondition.searchNull()) {
					String methodName = null;
					if(field == collectionField) {
						methodName = queryCondition.queryType().getSearchNullMethodName(getFieldName(collectionFieldName));
					}else {
						String singleFieldName = getSingleFieldName(queryCondition.fieldName());
						methodName = queryCondition.queryType().getSearchNullMethodName(StringUtils.hasLength(singleFieldName) ? singleFieldName : getFieldName(field.getName()) );
					}
					Method method = criteria.getClass().getMethod(methodName);
					method.invoke(criteria);
				}
				continue;
			}
			
			//添加like方法前后字符
			if(field.getType() == String.class && queryCondition.queryType() == QueryType.like) {
				StringBuffer sb = new StringBuffer();
				sb.append(queryCondition.prepend()).append(value).append(queryCondition.append());
				value = sb.toString();
			}
			
			//处理开始日期类型
			if(field.getType() == Date.class && queryCondition.formatType() == FormatType.start) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime((Date)value);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				value = calendar.getTime();
			}
			
			//处理结束日期类型
			if(field.getType() == Date.class && queryCondition.formatType() == FormatType.end) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime((Date)value);
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				value = calendar.getTime();
			}
			
			
			String methodName = null;
			if(field == collectionField) {
				methodName = queryCondition.queryType().getMethodName(getFieldName(collectionFieldName));
			}else {
				String singleFieldName = getSingleFieldName(queryCondition.fieldName());
				methodName = queryCondition.queryType().getMethodName(StringUtils.hasLength(singleFieldName) ? singleFieldName : getFieldName(field.getName()));
			}
			Method method = null;
			if(queryCondition.fieldClass() == null || queryCondition.fieldClass().length == 0) {
				if(cls != null) {
					method = criteria.getClass().getMethod(methodName,cls);
				}else {
					method = criteria.getClass().getMethod(methodName,field.getType());
				}
			}else {
				method = criteria.getClass().getMethod(methodName,queryCondition.fieldClass());
			}
			method.invoke(criteria, value);
		}
	}

	private static String getSingleFieldName(String[] fieldNames) {
		if(fieldNames == null) {
			return null;
		}
		
		if(fieldNames.length == 0) {
			return null;
		}
		String fieldName = fieldNames[0];
		
		if(!StringUtils.hasLength(fieldName)) {
			return null;
		}
		
		String firstChar = fieldName.substring(0, 1);
		fieldName = fieldName.replaceFirst(firstChar, firstChar.toUpperCase());
		return fieldName;
	}
	
	private static String getFieldName(String fieldName) {
		String firstChar = fieldName.substring(0, 1);
		fieldName = fieldName.replaceFirst(firstChar, firstChar.toUpperCase());
		return fieldName;
	}
	
}
