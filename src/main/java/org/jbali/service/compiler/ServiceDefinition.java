package org.jbali.service.compiler;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceDefinition {

	private final Map<String, Operation> ops = new HashMap<String, Operation>();
	private final String name;

	public ServiceDefinition(String name) {
		this.name = name;
	}

	public class Operation {
		
		/**
		 * The operation name
		 */
		private final String name;
		
		/**
		 * The type name of the input
		 */
		private final String input;
		
		/**
		 * The name of the input argument (only used for e.g. parameter names in Java interfaces)
		 */
		private final String inputName;
		
		/**
		 * The type name of the output
		 */
		private final String output;

		
		public Operation(String name, String input, String inputName, String output) {
			super();
			this.name = name;
			this.input = input;
			this.inputName = inputName;
			this.output = output;
		}
		
		public String getName() {
			return name;
		}
		
		public String getInput() {
			return input;
		}
		
		public String getOutput() {
			return output;
		}
		
		public String getInputName() {
			return inputName;
		}
		
	}

	public String getName() {
		return name;
	}
	
	public Operation getOperation(String name) {
		return ops.get(name);
	}
	
	public void addOperation(String opName, String input, String inputName, String retType) {
		ops.put(opName, new Operation(opName, input, inputName, retType));
	}
	
	@Override
	public String toString() {
		return "[ServiceDefinition " + name + "]";
	}
	
	public void print(PrintWriter wrt) {
		
		wrt.println("Service " + name);
		for (Operation op : ops.values()) {
			
			wrt.println("\t" + op.output + " " + op.name + "(" + op.input + ")");
			
		}
		
	}

	public Collection<? extends Operation> getOperations() {
		return ops.values();
	}
	
}
