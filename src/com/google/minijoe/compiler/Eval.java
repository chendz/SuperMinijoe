// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.minijoe.compiler;

import com.google.minijoe.compiler.ast.Program;
import com.google.minijoe.compiler.visitor.CodeGenerationVisitor;
import com.google.minijoe.compiler.visitor.DeclarationVisitor;
import com.google.minijoe.compiler.visitor.RoundtripVisitor;
import com.google.minijoe.samples.compiler.Crawler;
import com.google.minijoe.samples.compiler.Curl;
import com.google.minijoe.samples.compiler.PageRank;
import com.google.minijoe.samples.compiler.Readability;
import com.google.minijoe.sys.JsArray;
import com.google.minijoe.sys.JsFunction;
import com.google.minijoe.sys.JsObject;
import com.google.minijoe.sys.JsSystem;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.WebSitemapGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import se.rupy.http.Daemon;
import se.rupy.http.Event;
import se.rupy.http.Service;
import uk.org.freedonia.jfreewhois.Whois;
import uk.org.freedonia.jfreewhois.exceptions.HostNameValidationException;
import uk.org.freedonia.jfreewhois.exceptions.WhoisException;

/**
 * Simple facade for the parser and code generator
 *
 * @author Stefan Haustein
 */
public class Eval extends JsObject {
  static final int ID_EVAL = 100;
  static final int ID_HTTP_GET = 101;
  static final int ID_POST_JSON = 102;
  static final int ID_CRAWLER = 103;
  static final int ID_CURL = 104;
  static final int ID_EXTRACT_HTML = 105;
  static final int ID_ADD_RESPONSE = 106;
  static final int ID_COMPILE = 107;
  static final int ID_LOAD = 108;
  static final int ID_GEN_SITEMAP = 109;
  static final int ID_WHOIS = 110;
  static final int ID_PAGERANK = 111;
  
  private Daemon d = null;

  static final JsObject COMPILER_PROTOTYPE = new JsObject(OBJECT_PROTOTYPE);

  public Eval() {
    super(COMPILER_PROTOTYPE);
    scopeChain = JsSystem.createGlobal();
    addVar("eval", new JsFunction(ID_EVAL, 2));
    addVar("httpGet", new JsFunction(ID_HTTP_GET,1));
    addVar("postJson", new JsFunction(ID_POST_JSON,2));
    addVar("startCrawler", new JsFunction(ID_CRAWLER, 1));
    addVar("curl", new JsFunction(ID_CURL,1));
    addVar("extractHTML", new JsFunction(ID_EXTRACT_HTML, 1));
    addVar("addResponse", new JsFunction(ID_ADD_RESPONSE, 2));
    addVar("compile", new JsFunction(ID_COMPILE,1));
    addVar("genSiteMap", new JsFunction(ID_GEN_SITEMAP, 1));
    addVar("whois", new JsFunction(ID_WHOIS, 1));
    addVar("pagerank", new JsFunction(ID_PAGERANK, 1));
    
    //启动一个HTTP服务器
    try{
    	Properties p = new Properties();
    	p.put("test", "false");
    	p.put("verbose", "true");
    	p.put("log", "true");
    	p.put("panel", "false");    
        d = new Daemon(p);
        d.start();
    }catch(Exception ex){
    	ex.printStackTrace();
    }
  }

  public static JsObject createGlobal() {
    return new Eval();
  }

  public void evalNative(int index, JsArray stack, int sp, int parCount) {
    switch (index) {
    case ID_HTTP_GET:
		try {
	    	String url = stack.getString(sp+2);
	    	OkHttpClient client = new OkHttpClient();
	        Request request = new Request.Builder().url(url) .build();			
			Response response = client.newCall(request).execute();
		    stack.setObject(sp, response.body().string());    				
		} catch (IOException ex) {
			ex.printStackTrace();
		}

    	break;
   
    case ID_POST_JSON:
    	try{
				OkHttpClient client = new OkHttpClient();

				RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),stack.getString(sp + 3));
				Request request = new Request.Builder().url(stack.getString(sp + 2)).post(body).build();
				Response response = client.newCall(request).execute();
				stack.setObject(sp, response.body().string());    		
    	}catch(IOException ex){
    		ex.printStackTrace();
    	}
    	break;
    	
    	
    case ID_CRAWLER:
    	try{
             Crawler.startCrawler(stack.getString(sp+2));
    	}catch(IOException ex){
    		ex.printStackTrace();
    	}
    	break;
    	
    	
    case ID_CURL:
    	new Thread(new Curl()).start();
    	break;
    	
    	
    case ID_EXTRACT_HTML:
    	
		try {
			Readability readability = new Readability(new URL(stack.getString(sp+2)), stack.getInt(sp+3));
	    	readability.init();
	    	stack.setObject(sp, readability.outerHtml());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}  

    	break;

      case ID_EVAL:
        try {
          stack.setObject(
              sp,
              eval(stack.getString(sp + 2),
              stack.isNull(sp + 3) ? stack.getJsObject(sp) : stack.getJsObject(sp + 3))
          );
        } catch (Exception e) {
          throw new RuntimeException("" + e);
        }

        break;
        
        
      case ID_ADD_RESPONSE:
    	  try {
    		 final String mPath = stack.getString(sp+2);
    		 final String mOut = stack.getString(sp+3);
			d.add(
						new Service(){

							@Override
							public String path() {							
								return mPath;
							}

							@Override
							public void filter(Event event) throws Event, Exception {
								event.output().print(mOut);								
							}
							
						}
					  );
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    	  break;
    	  
    	  
      case ID_COMPILE:
    	  try{
    	    File file = new File(stack.getString(sp+2));
    	    DataInputStream dis = new DataInputStream(new FileInputStream(file));
    	    byte[] data = new byte[(int) file.length()];
    	    dis.readFully(data);
    	    String code = new String(data, "UTF-8");
    	    Eval.compile(code, System.out);    	  
    	  }catch(Exception ex){
    		  ex.printStackTrace();
    	  }
    	  break;
    	  
      case ID_LOAD:
    	  try{
      	    File file = new File(stack.getString(sp+2));
      	    DataInputStream dis = new DataInputStream(new FileInputStream(file));
      	    byte[] data = new byte[(int) file.length()];
      	    dis.readFully(data);
      	    String code = new String(data, "UTF-8");    	
      	    //加载xxx.js文件
      	    Eval.eval(code, Eval.createGlobal());
    	  }catch(Exception ex){
    		  ex.printStackTrace();
    	  }
    	break;
    	
    	
      case ID_GEN_SITEMAP:
    	  
    	  try{
    	    	// create web sitemap for web http://www.javavids.com
        	  WebSitemapGenerator webSitemapGenerator = new WebSitemapGenerator("http://www.javavids.com");
        	  // add some URLs
        	  webSitemapGenerator.addPage(new WebPage().setName("index.php")
        	                     .setPriority(1.0).setChangeFreq(ChangeFreq.NEVER).setLastMod(new Date()));
        	  webSitemapGenerator.addPage(new WebPage().setName("latest.php"));
        	  webSitemapGenerator.addPage(new WebPage().setName("contact.php"));
        	  // generate sitemap and save it to file /var/www/sitemap.xml
        	  File file = new File("/var/www/sitemap.xml");
        	  webSitemapGenerator.constructAndSaveSitemap(file);
        	  // inform Google that this sitemap has changed
        	  webSitemapGenerator.pingGoogle();    	 
    	  }catch(Exception ex){
    		  ex.printStackTrace();
    	  }
    	  break;
    	  
      case ID_WHOIS:
    	  try {
			stack.setObject(sp, Whois.getRawWhoisResults(stack.getString(sp+2)));
		} catch (WhoisException e) {
			e.printStackTrace();
		} catch (HostNameValidationException e) {
			e.printStackTrace();
		}
    	  break;
    	  
    	  
      case ID_PAGERANK:
    	  stack.setObject(sp, PageRank.getPR(stack.getString(sp+2)));
    	  break;

      default:
        super.evalNative(index, stack, sp, parCount);
    }
  }

  public static void compile(String input, OutputStream os) throws CompilerException, IOException {
    Lexer lexer = new Lexer(input);
    Parser parser = new Parser(lexer);

    Program program = parser.parseProgram();

    if (Config.DEBUG_SOURCE) {
      Writer w = new OutputStreamWriter(System.out);
      new RoundtripVisitor(w).visit(program);
      w.flush();
    }

    // handle variable and function declarations
    new DeclarationVisitor().visit(program);

    DataOutputStream dos = new DataOutputStream(os);
    new CodeGenerationVisitor(dos).visit(program);
    dos.flush();
  }
  
  public static Object eval(String input, JsObject context) throws CompilerException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    compile(input, baos);
    byte[] code = baos.toByteArray();

    if (Config.DEBUG_DISSASSEMBLY) {
      new Disassembler(new DataInputStream(new ByteArrayInputStream(code))).dump();
    }

    return JsFunction.exec(new DataInputStream(new ByteArrayInputStream(code)), context);
  }
}
