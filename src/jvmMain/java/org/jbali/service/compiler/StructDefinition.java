package org.jbali.service.compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class StructDefinition {

	public static class StructField {

		private final String type;
		private final String name;

		public StructField(String name, String type) {
			this.name = name;
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	private final List<StructField> fields = new ArrayList<StructField>();
	private final String name;
	
	public StructDefinition(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void addField(String name, String type) {
		fields.add(new StructField(name, type));
	}

	public void print(PrintWriter wrt) {
		wrt.println("Struct " + name);
		for (StructField f : fields) {
			wrt.println("\t" + f.type + " " + f.name);
		}
	}
	
	public List<? extends StructField> getFields() {
		return fields;
	}
	
}
