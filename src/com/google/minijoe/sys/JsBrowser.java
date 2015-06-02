package com.google.minijoe.sys;

public class JsBrowser extends JsObject {
	
	   static final JsObject BROWSER_PROTOTYPE = 
			    new JsObject(JsObject.OBJECT_PROTOTYPE);	

	public JsBrowser(JsObject __proto__) {
		super(BROWSER_PROTOTYPE);
		// TODO Auto-generated constructor stub
	}

	public void evalNative(int index, JsArray stack, int sp, int parCount) {
		switch (index) {

		default:
			super.evalNative(index, stack, sp, parCount);
		}
	}

}
