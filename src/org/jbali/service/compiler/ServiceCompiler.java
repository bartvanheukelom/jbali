package org.jbali.service.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.jbali.service.compiler.ServiceDefinitionParser.ParseResult;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class ServiceCompiler {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		if (args.length != 3) {
			System.out.println("Usage: java -jar service-compiler.jar SERVICEFILE PACKAGE OUTPUTDIR");
			System.exit(1);
		}
		
		final String serviceFile = args[0];
		final String pakage = args[1];
		final File outputDir = new File(args[2], pakage.replace('.', '/'));
		
		ParseResult parseResult = new ServiceDefinitionParser().parse(new FileReader(serviceFile));
		
		final JavaGenerator gen = new JavaGenerator();
		
		PrintWriter o = new PrintWriter(System.out, true);

		for (ServiceDefinition svc : parseResult.getServices()) {
			svc.print(o);
			writeCode(outputDir, o, gen.generateHandler(svc, pakage));
			writeCode(outputDir, o, gen.generateEndpoint(svc, pakage));
		}
		
		for (StructDefinition str : parseResult.getStructs()) {
			str.print(o);
			writeCode(outputDir, o, gen.generateStructMutable(str, pakage));
		}
		
		System.out.println("Done");
		System.out.flush();
		
	}

	private static void writeCode(final File outputDir, PrintWriter o, GeneratedCode code) throws IOException {
		
		File output = new File(outputDir, code.getClassName() + ".java");
//		o.println(output + ":");
//		o.println(code.getCode());
		Files.write(code.getCode(), output, Charsets.UTF_8);
		
	}

}
