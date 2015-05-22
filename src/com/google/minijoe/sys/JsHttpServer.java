package com.google.minijoe.sys;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import se.rupy.http.Daemon;
import se.rupy.http.Event;
import se.rupy.http.Service;

/**
 * 定义httpserver类
 * @author chendz
 *
 */
public class JsHttpServer extends JsObject {
	
   static final int ID_START = 117;
   static final int ID_ADD_OUTPUT = 106;
   static final int ID_CREATE = 118;
   static final int ID_STOP = 119;
	
   static final JsObject HTTPSERVER_PROTOTYPE = 
			    new JsObject(JsObject.OBJECT_PROTOTYPE);



   private Daemon httpserver = null;
   
	
   public JsHttpServer(){
	    super(HTTPSERVER_PROTOTYPE);

	    addVar("start", new JsFunction(ID_START, 1));
	    addVar("stop", new JsFunction(ID_STOP, 0));
	    addVar("addOutput", new JsFunction(ID_ADD_OUTPUT, 2));
	    addVar("create", new JsFunction(ID_CREATE, 0));
	    
		Properties p = new Properties();
		p.put("test", "false");
		p.put("verbose", "true");
		p.put("log", "true");
		p.put("panel", "false");	
		httpserver = new Daemon(p);		
		


		
		//关联下某些目录路径
		final String ROOT_PATH = System.getProperty("user.dir")+"/web";
		File ff = new File(ROOT_PATH);
		if (!ff.isDirectory()){
			ff.mkdir();
		}			
	    try{
		String[] files = new File(ROOT_PATH).list();	
		for(String f: files){
			final String name = f;
			httpserver.add(
					new Service(){
						@Override
						public String path(){
							return "/"+name;
						}
						
						@Override
						public void filter(Event event) throws Event, Exception {
						    //读取文件，然后创建新的上下文？
							String js = readFile(ROOT_PATH +"\\"+ name);
							event.output().print(js);
						}
			
					}
            );
		}	
		
		//测试用
		httpserver.add(new Service() {
			public String path() {
				return "/ping";
			}

			public void filter(Event event) throws Event, Exception {
				event.output().println("<p>Ping OK</p>");
			}
		});		
	    }catch(Exception ex){
	    	ex.printStackTrace();
	    }
   }
   
   public void evalNative(int index, JsArray stack, int sp, int parCount) {
	    switch (index) {
	      case ID_ADD_OUTPUT:
	    	  try {
	    		 final String mPath = stack.getString(sp+2);
	    		 final String mOut = stack.getString(sp+3);
				 httpserver.add(
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
	    	  
	      case ID_START:
	    	 try{
	    	  if (httpserver != null){
	    		  httpserver.start();
	    	  }
	    	 }catch(Exception ex){
	    		 ex.printStackTrace();
	    	 }
	    	 break;
	    	 
	      case ID_STOP:
	    	 try{
	    	  if (httpserver != null){
	    		  httpserver.stop();
	    	  }
	    	 }catch(Exception ex){
	    		 ex.printStackTrace();
	    	 }
	    	 break;	    	 
	    	 
	    	 
	      case ID_CREATE:
	    	  System.out.println("HttpServer.create();");
	    	  break;	    	 
	    	  	    	  
	      default:
	          super.evalNative(index, stack, sp, parCount);	    	  
	    }
   }
   
	public static String readFile(String file) throws IOException {
		// jdk7写法
		try (FileInputStream fis = new FileInputStream(file)) {
			return new String(toByteArray(fis));
		}
	}
	
	public static byte[] toByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[128];
		int count = 0;
		while ((count = is.read(buf, 0, buf.length)) != -1) {
			baos.write(buf, 0, count);
		}
		return baos.toByteArray();
	}	   
}
