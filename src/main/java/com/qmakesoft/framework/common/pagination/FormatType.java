package com.qmakesoft.framework.common.pagination;

public enum FormatType {

	empty("empty"),
	start("start"),
	end("end");
	public String type;
	FormatType(String type){
		this.type = type;
	}
}
