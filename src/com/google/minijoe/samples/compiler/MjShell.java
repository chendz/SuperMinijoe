package com.google.minijoe.samples.compiler;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.minijoe.compiler.CompilerException;
import com.google.minijoe.compiler.Eval;
import com.google.minijoe.sys.JsObject;

public class MjShell {

	public static void main(String[] args) {
		JsObject global = Eval.createGlobal();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter err = new PrintWriter(System.err);

		while (true) {
			System.err.print("jjs>");

			String source = "";
			try {
				source = in.readLine();
			} catch (IOException ioe) {
				err.println(ioe.toString());
			}

			if (source == null) {
				break;
			}
			
			if ("exit".equals(source)){
				break;
			}

			if (source.isEmpty()) {
				continue;
			}

			try {
				System.out.println("----------");
				System.out.println(Eval.eval(source, global));
			} catch (Exception e) {
				err.println(e);
			}
		}

	}

}
