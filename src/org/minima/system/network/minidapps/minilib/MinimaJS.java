package org.minima.system.network.minidapps.minilib;

import org.minima.system.network.commands.CMD;
import org.minima.system.network.commands.SQL;
import org.minima.utils.MinimaLogger;
import org.mozilla.javascript.Function;

public class MinimaJS {
	
	/**
	 * File functions..
	 */
	public JSFile file;
	
	/**
	 * NET Functions
	 */
	public JSNet net;
	
	/**
	 * UTIL Functions
	 */
	public JSUtil util;
	
	/**
	 * JS BACKEND link
	 */
	private BackEndDAPP mBackBone;
	
	public MinimaJS(BackEndDAPP zBackBone) {
		mBackBone = zBackBone;
		
		file = new JSFile(mBackBone);
		net  = new JSNet(mBackBone);
		util = new JSUtil();
	}
	
	/**
	 * Log data to Standard out
	 * @param zLog
	 */
	public void log(String zLog) {
		MinimaLogger.log("["+mBackBone.getMiniDAPPID()+"] "+zLog);
	}
	
	/**
	 * Main Minima Command
	 * 
	 * @param zCommand
	 */
	public void cmd(String zCommand) {
		cmd(zCommand,null);
	}
	
	public void cmd(String zCommand, Function zCallback) {
		MinimaLogger.log("MinimaJS CMD - "+zCommand+" "+zCallback);
		
		//Create a Command 
		CMD cmd = null;
		if(zCallback != null) {
			cmd = new CMD(zCommand, zCallback, mBackBone.getContext(), mBackBone.getScope());	
		}else {
			cmd = new CMD(zCommand);
		}

		//Run it.. synchronous..
		cmd.run();
//		Thread cmdthread = new Thread(cmd);
//		cmdthread.start();
	}
	
	
	/**
	 * Main SQL function
	 * 
	 * @param zCommand
	 */
	public void sql(String zCommand) {
		sql(zCommand, null);
	}
	
	public void sql(String zCommand, Function zCallback) {
		MinimaLogger.log("MinimaJS SQL -"+zCommand);
		
		//Create a SQL command
		SQL sql = null;
		if(zCallback != null) {
			sql = new SQL(zCommand, mBackBone.getMiniDAPPID(), zCallback, 
					mBackBone.getContext(), mBackBone.getScope());
		}else {
			sql = new SQL(zCommand, mBackBone.getMiniDAPPID());
		}
		
		//Run it.. synchronous
		sql.run();
//		Thread sqlthread = new Thread(sql);
//		sqlthread.run();
	}	
}
