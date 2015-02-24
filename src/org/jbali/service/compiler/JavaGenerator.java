package org.jbali.service.compiler;

import java.util.List;
import java.util.Map;

import org.jbali.collect.Maps;
import org.jbali.service.BaseServiceHandler;
import org.jbali.service.compiler.ServiceDefinition.Operation;
import org.jbali.service.compiler.StructDefinition.StructField;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

public class JavaGenerator {

	private final Map<String, Class<?>> typeMap = Maps.<String,Class<?>>createHash(
			"long", Long.class,
			"string", String.class,
			"int", Integer.class,
			"jsonobject", JsonObject.class
	);
	
	// TODO pakage = field
	
	public GeneratedCode generateEndpoint(ServiceDefinition svc, String pakage) {
		
		String className = svc.getName() + "Endpoint";
		
		StringBuilder src = new StringBuilder();
		src.append("package " + pakage + ";\n\n");
		
		src.append("public interface " + className + " {\n");
		
		for (Operation o : svc.getOperations()) {
			
			src.append(javaTypeName(o.getOutput()) + " " + o.getName() + "(");
			if (o.getInput() != null)
				src.append(javaTypeName(o.getInput()) + " " + o.getInputName());
			src.append(");\n");
			
		}
		
		src.append("}\n");
		
		return new GeneratedCode(pakage, className, src.toString());
		
	}
	
	public GeneratedCode generateHandler(ServiceDefinition svc, String pakage) {
		
		String className = svc.getName() + "Handler";
		
		StringBuilder src = new StringBuilder();
		src.append("package " + pakage + ";\n\n");
		
		src.append("public class " + className + " extends " + BaseServiceHandler.class.getName() + " {\n");
		
		final String epName = svc.getName() + "Endpoint";
		final String opHandler = BaseServiceHandler.class.getName() + ".OperationHandler";
		
		src.append("public " + className + "(final " + epName + " impl) {\n");
		src.append("super(" + ImmutableMap.class.getName() + ".copyOf(" + Maps.class.getName() + ".<String,"+opHandler+">createHash(\n");
		
		// operations
		boolean first = true;
		for (Operation o : svc.getOperations()) {
			
			if (first) {
				first = false;
			} else {
				src.append(",");
			}
			
			// handler class
			src.append("\n\"" + o.getName() + "\", new " + opHandler + "() {\n");
			
			// handle()
			src.append("@Override public Object handle(Object input) {\n");
			boolean retVoid = o.getOutput().equals("void");
			if (!retVoid) {
				src.append("return ");
			}
			src.append("impl." + o.getName() + "(");
			if (o.getInput() != null)
				src.append("(" + javaTypeName(o.getInput()) + ") input");
			src.append(");\n");
			if (retVoid) {
				src.append("return null;\n");
			}
			src.append("}\n");
			
			// getInputType()
			src.append("@Override public Class<?> getInputType() {\n");
			if (o.getInput() == null) {
				src.append("return null;\n");
			} else {
				src.append("return " + javaTypeName(o.getInput()) + ".class;\n");
			}
			src.append("}\n");
			
			// end of class
			src.append("}\n\n");
			
		}
		
		src.append("))); }\n");
		
		src.append("}\n");
		
		return new GeneratedCode(pakage, className, src.toString());
		
	}
	
	public GeneratedCode generateStructImmutable(StructDefinition struct, String pakage) {
		
		StringBuilder src = new StringBuilder();
		src.append("package " + pakage + ";\n\n");
		src.append("public class " + struct.getName() + " {\n");
		
		final List<? extends StructField> fields = struct.getFields();
		
		// fields
		for (StructField f : fields) {
			src.append("\tprivate final " + javaTypeName(f.getType()) + " " + f.getName() + ";\n");
		}
		
		// constructor
		src.append("\n\n\tpublic " + struct.getName() + "(\n");
		boolean first = true;
		for (StructField f : fields) {
			src.append("\t\t");
			if (first) {
				first = false;
			} else {
				src.append(",");
			}
			src.append(javaTypeName(f.getType()) + " " + f.getName() + "\n");
		}
		src.append("\t) {\n");
		for (StructField f : fields) {
			src.append("\t\tthis." + f.getName() + " = " + f.getName() + ";\n");
		}
		src.append("\t}\n\n\n");
		
		// getters
		for (StructField f : fields) {
			final String upName = Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
			src.append("\tpublic " + javaTypeName(f.getType()) + " get" + upName + "() { return " + f.getName() + "; }\n");
		}
		src.append("}\n");
		
		return new GeneratedCode(pakage, struct.getName(), src.toString());
		
	}
	
	public GeneratedCode generateStructMutable(StructDefinition struct, String pakage) {
		
		StringBuilder src = new StringBuilder();
		src.append("package " + pakage + ";\n\n");
		src.append("public class " + struct.getName() + " {\n");
		
		final List<? extends StructField> fields = struct.getFields();
		
		// fields
		for (StructField f : fields) {
			src.append("\tprivate " + javaTypeName(f.getType()) + " " + f.getName() + ";\n");
		}
		
		// no-arg constructor
		src.append("\n\n\tpublic " + struct.getName() + "() {}\n");
		
		// constructor with data
		src.append("\n\n\tpublic " + struct.getName() + "(\n");
		boolean first = true;
		for (StructField f : fields) {
			src.append("\t\t");
			if (first) {
				first = false;
			} else {
				src.append(",");
			}
			src.append(javaTypeName(f.getType()) + " " + f.getName() + "\n");
		}
		src.append("\t) {\n");
		for (StructField f : fields) {
			src.append("\t\tthis." + f.getName() + " = " + f.getName() + ";\n");
		}
		src.append("\t}\n\n\n");
	
		// getters & setters
		for (StructField f : fields) {
			final String upName = Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
			src.append("\tpublic " + javaTypeName(f.getType()) + " get" + upName + "() { return " + f.getName() + "; }\n");
			src.append("\tpublic void set" + upName + "(" + javaTypeName(f.getType()) + " val) { " + f.getName() + " = val; }\n");
		}
		src.append("}\n");
		
		return new GeneratedCode(pakage, struct.getName(), src.toString());
		
	}

	private String javaTypeName(final String type) {
		final Class<?> t = typeMap.get(type);
		if (t == null) return type;
		return t.getName();
	}
	
}
