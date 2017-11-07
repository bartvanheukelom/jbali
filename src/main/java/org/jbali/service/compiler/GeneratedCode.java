package org.jbali.service.compiler;

public class GeneratedCode {

	private final String pakage;
	private final String className;
	private final String code;

	public GeneratedCode(String pakage, String className, String code) {
		this.pakage = pakage;
		this.className = className;
		this.code = code;
	}
	
	public String getPackage() {
		return pakage;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getCode() {
		return code;
	}
	
}
