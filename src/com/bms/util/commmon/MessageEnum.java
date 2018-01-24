package com.bms.util.commmon;

public enum MessageEnum {
	  	NO_INFO_WAS_FOUND("未查询到相关信息"),
	    QUERY_INFO_FAILED("查询信息失败"),
	    PARAMETER_FORMAT_ERROR("参数格式校验错误"),
	    PARAMETER_DOES_NOT_EXIST("参数不存在"),
	    CONVERSION_DATA_FAILED("转换数据失败"),
	    ENCODING_FAILED("编码失败"),
	    DECODING_FAILED("解码失败"),
	    DES_ENCRYPTION_FAILED("DES加密失败"),
	    DES_DECRYPTION_FAILED("DES解密失败"),
	    BASE64_ENCODING_FAILED("BASE64编码失败"),
	    BASE64_DECODING_FAILED("BASE64解码失败"),
	    SIGNATURE_INCONSISTENCY("签名校验失败，签名不一致"),
	    SIGNATURE_VERIFICATION_FAILED("签名校验失败"),
	    ILLEGAL_REQUEST("非法请求"),
	    HIGH_FREQUENCY("请求频率过高，请稍后重试"),
	    REQUEST_LINK_FAILED("请求链接已失效");

	    private String message;

	    private MessageEnum(String message) {
	        this.message = message;
	    }

	    public String getMessage() {
	        return this.message;
	    }

	    public void setMessage(String message) {
	        this.message = message;
	    }
}
