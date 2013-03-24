package com.example.simpleandroidarduinobtconnection;

import android.content.Context;
import android.widget.Toast;

public class Log {

	Context ctxt;
	MainActivity main;
	
	public Log(MainActivity main, Context ctxt){
		this.ctxt = ctxt;
		this.main = main;
	}
	
	/** prints a msg on the UI screen **/
	public void println(String msg){
		final String out = msg;
		main.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(ctxt, out, Toast.LENGTH_SHORT).show();
				
			}
		});
	}
	
	public void printToConsole(String msg){
		final String out = msg;
		main.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				main.printToConsole(out);
			}
		});
	}
	
	public void tag(String msg) {
		android.util.Log.d("BT-for-STK", msg);
	}

}