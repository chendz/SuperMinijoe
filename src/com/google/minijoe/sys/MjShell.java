package com.google.minijoe.sys;



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import se.rupy.http.Daemon;
import se.rupy.http.Event;
import se.rupy.http.Service;

import com.google.minijoe.compiler.CompilerException;

public class MjShell {
	

	
	public MjShell(){						
	}

	
	
	public void run(){
		
		//应该是唯一的上下文
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

	public static void main(String[] args) {
        MjShell shell = new MjShell();
        shell.run();
	}

}
