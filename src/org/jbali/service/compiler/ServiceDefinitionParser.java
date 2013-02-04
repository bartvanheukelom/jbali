package org.jbali.service.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceDefinitionParser {

	public static class ParseResult {
		
		private final Set<ServiceDefinition> services = new HashSet<ServiceDefinition>();
		private final Set<StructDefinition> structs = new HashSet<StructDefinition>();

		public Set<? extends ServiceDefinition> getServices() {
			return services;
		}
		
		public Set<? extends StructDefinition> getStructs() {
			return structs;
		}
		
	}
	
	private static final int CT_ROOT = 0;
	private static final int CT_SERVICE = 1;
	private static final int CT_STRUCT = 2;
	
	public ParseResult parse(Reader src) throws IOException {
		
		ParseResult res = new ParseResult();
		
		BufferedReader reader = new BufferedReader(src);
		
		int context = 0;
		
		ServiceDefinition svc = null;
		StructDefinition curStruct = null;
		
		int lineNo = -1;
		String line;
		while ((line = reader.readLine()) != null) {
			
			lineNo++;
			
//			System.out.println("::: "+line);
			
			String[] parts = parseLine(line);
			
//			for (String p : parts)
//				System.out.println("  >"+p+"<");
			
			if (parts.length == 0) continue;
			
			if (context == CT_ROOT) {
				if (parts[0].equals("service")) {
					if (!parts[2].equals("{")) throw new RuntimeException("[" + lineNo + "] Expected '{' after service name");
					svc = new ServiceDefinition(parts[1]);
					res.services.add(svc);
					context++;
				}
			}
			else if (context == CT_SERVICE) {
				
				// if "}", end service
				if (parts[0].equals("}")) {
					context--;
					svc = null;
					continue;
				}
				
				// --- else, read operation definition
				
				String retType = parts[0];
				String opName = parts[1];
				
				if (retType.equals("struct")) {
					curStruct = new StructDefinition(opName);
					res.structs.add(curStruct);
					context++;
					
				} else {
				
					// --- read args
					if (parts.length <= 2 || !parts[2].equals("("))
						throw new RuntimeException("[" + lineNo + "] Expected '(' after operation name " + svc.getName() + "::" + opName);
					
					List<String[]> args = new ArrayList<String[]>();
					if (!parts[3].equals(")")) for (int a = 0; true; a += 3) {
						
//						System.out.println(parts[3+a] + " " + parts[4+a]);
						
						
						if (parts.length <= 3+a)
							throw new RuntimeException("[" + lineNo + "] Expected type of argument " + svc.getName() + "::" + opName + "#" + a + " but found end of line");
						final String type = parts[3+a];
						if (type.equals(")") || type.equals(","))
							throw new RuntimeException("[" + lineNo + "] Expected type of argument " + svc.getName() + "::" + opName + "#" + a + " but found '" + type + "'");
						
						if (parts.length <= 4+a)
							throw new RuntimeException("[" + lineNo + "] Expected name of argument " + svc.getName() + "::" + opName + "#" + a + " but found end of line");
						final String name = parts[4+a];
						if (name.equals(")") || name.equals(","))
							throw new RuntimeException("[" + lineNo + "] Expected name of argument " + svc.getName() + "::" + opName + "#" + a + " but found '" + name + "'");
						
						args.add(new String[]{type, name});
						
						if (parts[5+a].equals(")")) {
							break;
						} else if (!parts[5+a].equals(",")) {
							throw new RuntimeException(
									"[" + lineNo + "] Expected ',' or ')' after argument " + svc.getName() + "::" + opName + "(" + name + ")");
						}
						
					}
	
					// --- determine input type
					
					String input;
					String inputName = null;
					// if no args, input is void
					if (args.size() == 0) {
						input = null;
					}
					// if 1 arg, that is the input
					else if (args.size() == 1) {
						input = args.get(0)[0];
						inputName = args.get(0)[1];
					}
					// if more, generate a struct for it
					else {
						StructDefinition struct = new StructDefinition(Character.toUpperCase(opName.charAt(0))+opName.substring(1)+"Input");
						for (String[] a : args) {
							struct.addField(a[1], a[0]);
						}
						input = struct.getName();
						inputName = "input";
						res.structs.add(struct);
					}
					
					svc.addOperation(opName, input, inputName, retType);
				
				}
				
			}
			else if (context == CT_STRUCT) {
				
				// if "}", end struct
				if (parts[0].equals("}")) {
					context--;
					curStruct = null;
					continue;
				}
				
				String retType = parts[0];
				String fieldName = parts[1];
				
				curStruct.addField(fieldName, retType);
				
				
			}
			else {
				throw new RuntimeException(String.valueOf(context));
			}
			
		}
		
		return res;
		
	}
	
	private static String[] parseLine(String line) {
		
		if (line.trim().isEmpty()) return new String[0];
		
		List<StringBuilder> parts = new ArrayList<StringBuilder>();
		
		for (char c : line.toCharArray()) {
			
			if (parts.size() == 0) {
				parts.add(new StringBuilder());
			}
			
			StringBuilder word = parts.get(parts.size()-1);
			
			if ("(){},".indexOf(c) != -1) {
				if (word.length() != 0) {
					parts.add(new StringBuilder());
				}
				parts.get(parts.size()-1).append(c);
				parts.add(new StringBuilder());
			}
			else if (Character.isWhitespace(c)) {
				if (word.length() != 0) parts.add(new StringBuilder());
			}
			else if (c == '#') break;
			else word.append(c);
			
			
		}
		
		if (parts.get(parts.size()-1).length() == 0) parts.remove(parts.size()-1);
		
		String[] res = new String[parts.size()];
		int i = 0;
		for (StringBuilder w : parts) {
			res[i] = w.toString();
			i++;
		}
		
		return res;
		
	}
	
}
