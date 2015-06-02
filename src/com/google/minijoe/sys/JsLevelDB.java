package com.google.minijoe.sys;

import java.io.File;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import org.iq80.leveldb.impl.Iq80DBFactory;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;
/**
 * 来自https://github.com/dain/leveldb的LevelDB
 * @author chendz
 *
 */
public class JsLevelDB extends JsObject {

	static final JsObject LEVEL_PROTOTYPE = new JsObject(
			JsObject.OBJECT_PROTOTYPE);

	public JsLevelDB() {
		super(LEVEL_PROTOTYPE);
		
		
	//-----------------------------	
    try{
		Options options = new Options();
		options.createIfMissing(true);
		Iq80DBFactory factory = org.iq80.leveldb.impl.Iq80DBFactory.factory;
		DB db = factory.open(new File("example"), options);
		try {

			db.put(bytes("Tampa"), bytes("rocks"));
			String value = asString(db.get(bytes("Tampa")));
			
			//WriteOptions wo = new WriteOptions();
			//db.delete(bytes("Tampa"), wo);
			
			
			String stats = db.getProperty("leveldb.stats");
			System.out.println(stats);
			
			
			DBIterator iterator = db.iterator();
			try {
			  for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
			    String key = asString(iterator.peekNext().getKey());
			    String val = asString(iterator.peekNext().getValue());
			    System.out.println(key+" = "+val);
			  }
			} finally {
			  // Make sure you close the iterator to avoid resource leaks.
			  iterator.close();
			}			
		} finally {
		  // Make sure you close the db to shutdown the 
		  // database and avoid resource leaks.
		  db.close();
		}		
		
    }catch(Exception ex){
    	ex.printStackTrace();
    }
    
    
	}

	public void evalNative(int index, JsArray stack, int sp, int parCount) {
		switch (index) {

		default:
			super.evalNative(index, stack, sp, parCount);
		}
	}
	
	public String toString(){
		return "LevelDB: 一个简单的NoSQL数据库。";
	}
}
