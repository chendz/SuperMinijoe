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

package com.google.minijoe.sys;

import com.google.minijoe.compiler.CodeGenerationVisitor;
import com.google.minijoe.compiler.CompilerException;
import com.google.minijoe.compiler.Config;
import com.google.minijoe.compiler.DeclarationVisitor;
import com.google.minijoe.compiler.Disassembler;
import com.google.minijoe.compiler.Lexer;
import com.google.minijoe.compiler.Parser;
import com.google.minijoe.compiler.RoundtripVisitor;
import com.google.minijoe.compiler.ast.Program;
import com.guilhermechapiewski.fluentmail.email.EmailMessage;
import com.guilhermechapiewski.fluentmail.transport.EmailTransportConfiguration;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.WebSitemapGenerator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.rupy.http.Daemon;
import se.rupy.http.Event;
import se.rupy.http.Service;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import uk.org.freedonia.jfreewhois.Whois;
import uk.org.freedonia.jfreewhois.exceptions.HostNameValidationException;
import uk.org.freedonia.jfreewhois.exceptions.WhoisException;

/**
 * Simple facade for the parser and code generator
 *
 * @author Stefan Haustein
 */
public class Eval extends JsObject {
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  
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
  static final int ID_SEND_TWITTER = 112;
  static final int ID_EXTRACT_TEXT = 113;
  static final int ID_LIST_LINKS = 114;
  static final int ID_LOG = 115;
  //发送邮件
  static final int ID_SEND_MAIL = 116;
  
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
    addVar("sendTwitter", new JsFunction(ID_SEND_TWITTER, 1));
    addVar("extractText", new JsFunction(ID_EXTRACT_TEXT, 2));
    addVar("listLinks", new JsFunction(ID_LIST_LINKS, 0));
    addVar("log", new JsFunction(ID_LOG, 1));
    addVar("sendMail", new JsFunction(ID_SEND_MAIL, 5));
    
    
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

    	  
      case ID_SEND_TWITTER:
          try {
              Twitter twitter = new TwitterFactory().getInstance();
              try {
                  // get request token.
                  // this will throw IllegalStateException if access token is already available
                  RequestToken requestToken = twitter.getOAuthRequestToken();
                  System.out.println("Got request token.");
                  System.out.println("Request token: " + requestToken.getToken());
                  System.out.println("Request token secret: " + requestToken.getTokenSecret());
                  AccessToken accessToken = null;

                  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                  while (null == accessToken) {
                      System.out.println("Open the following URL and grant access to your account:");
                      System.out.println(requestToken.getAuthorizationURL());
                      System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                      String pin = br.readLine();
                      try {
                          if (pin.length() > 0) {
                              accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                          } else {
                              accessToken = twitter.getOAuthAccessToken(requestToken);
                          }
                      } catch (TwitterException te) {
                          if (401 == te.getStatusCode()) {
                              System.out.println("Unable to get the access token.");
                          } else {
                              te.printStackTrace();
                          }
                      }
                  }
                  System.out.println("Got access token.");
                  System.out.println("Access token: " + accessToken.getToken());
                  System.out.println("Access token secret: " + accessToken.getTokenSecret());
              } catch (IllegalStateException ie) {
                  // access token is already available, or consumer key/secret is not set.
                  if (!twitter.getAuthorization().isEnabled()) {
                      System.out.println("OAuth consumer key/secret is not set.");
                      System.exit(-1);
                  }
              }
              Status status = twitter.updateStatus(stack.getString(sp+2));
              System.out.println("Successfully updated the status to [" + status.getText() + "].");
              System.exit(0);
          } catch (TwitterException te) {
              te.printStackTrace();
              System.out.println("Failed to get timeline: " + te.getMessage());
              System.exit(-1);
          } catch (IOException ioe) {
              ioe.printStackTrace();
              System.out.println("Failed to read the system input.");
              System.exit(-1);
          }
          break;
          
      case ID_EXTRACT_TEXT:    	  
			try {
				String url = stack.getString(sp + 2);
				String selector = stack.getString(sp + 3);

				Document doc = Jsoup.connect(url).userAgent("okhttp")
						.timeout(5 * 1000).get();

				HtmlToPlainText formatter = new HtmlToPlainText();

				if (selector != null) {
					Elements elements = doc.select(selector);
					StringBuffer sb = new StringBuffer();
					for (Element element : elements) {
						String plainText = formatter.getPlainText(element);
						sb.append(plainText);
					}
					stack.setObject(sp, sb.toString());
				} else {
					stack.setObject(sp, formatter.getPlainText(doc));

				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			break;
			
			
      case ID_LIST_LINKS:
    	  try{
    	        String url = stack.getString(sp+2);
    	        print("Fetching %s...", url);

    	        Document doc = Jsoup.connect(url).get();
    	        Elements links = doc.select("a[href]");
    	        Elements media = doc.select("[src]");
    	        Elements imports = doc.select("link[href]");

    	        print("\nMedia: (%d)", media.size());
    	        for (Element src : media) {
    	            if (src.tagName().equals("img"))
    	                print(" * %s: <%s> %sx%s (%s)",
    	                        src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"),
    	                        trim(src.attr("alt"), 20));
    	            else
    	                print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
    	        }

    	        print("\nImports: (%d)", imports.size());
    	        for (Element link : imports) {
    	            print(" * %s <%s> (%s)", link.tagName(),link.attr("abs:href"), link.attr("rel"));
    	        }

    	        print("\nLinks: (%d)", links.size());
    	        for (Element link : links) {
    	            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
    	        }    		  
    	  }catch(Exception ex){
    		  ex.printStackTrace();
    	  }
    	  break;
    	  
    	  
      case ID_LOG:
    	  log.info(stack.getString(sp+2));
    	  break;
    	  
    	  
      case ID_SEND_MAIL:
    	try{
  		// put your e-mail address here
  		final String yourAddress = "guilherme.@gmail.com";

  		// configure programatically your mail server info
  		EmailTransportConfiguration.configure("smtp.server.com", true,
  				false, "username", "password");

  		// and go!
  		new EmailMessage().from("demo@guilhermechapiewski.com").to(yourAddress)
  				.withSubject("Fluent Mail API")
  				.withAttachment("file_name")
  				.withBody("Demo message").send();

    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
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
  
  
  private static void print(String msg, Object... args) {
      System.out.println(String.format(msg, args));
  }

  private static String trim(String s, int width) {
      if (s.length() > width)
          return s.substring(0, width-1) + ".";
      else
          return s;
  }  
}
