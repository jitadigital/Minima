package org.minima.system.network.minidapps.minibackend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.StringTokenizer;

import org.minima.system.network.commands.CMD;
import org.minima.system.network.commands.FILE;
import org.minima.system.network.commands.NET;
import org.minima.system.network.commands.SQL;
import org.minima.system.network.minidapps.minilib.MiniLibUtility;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.mozilla.javascript.Function;

/**
 * This class handles a single request then exits
 * 
 * @author spartacusrex
 *
 */
public class MinimaJSBridge {

	/**
	 * JS BACKEND link
	 */
	private BackEndDAPP mBackBone;
	
	
	/**
	 * Main Constructor
	 * @param zSocket
	 */
	public MinimaJSBridge(BackEndDAPP zBackBone) {
		mBackBone = zBackBone;
	}

	public void post(String zType, String zData, Function zCallback) {
		
		MinimaLogger.log("MinimaJSBridge : "+zType+" "+zData);
		
		JSONObject res = new JSONObject();
		res.put("type", "answer");
		res.put("value", 43);
		
		//The response returned..
		String finalResult = res.toString();
		
		//Create a native JSON
		Object json = MiniLibUtility.makeJSONObject(finalResult, mBackBone.getContext(), mBackBone.getScope());
		
		//Make a function variable list
		Object functionArgs[] = { json };
	    
		//Call the function..
		zCallback.call(mBackBone.getContext(), mBackBone.getScope(), mBackBone.getScope(), functionArgs);
	
		/*
		String finalresult = "";
		
		//Is this a SQL function
		if(zType.equals("sql")) {
			//Create a SQL object
			SQL sql = new SQL(zData, zMiniDAPPID);
			
			//Run it..
			sql.run();
			
			//Get the Response..
        	finalresult = sql.getFinalResult();
			
		}else if(zType.equals("cmd")) {
			CMD cmd = new CMD(zData);
        	
        	//Run it..
            cmd.run();
 
            //Get the Response..
        	finalresult = cmd.getFinalResult();
		
		}else if(zType.equals("file")) {
			//File access..
			FILE file = new FILE(zData, zMiniDAPPID);
			
			//Run it..
			file.run();
			
			//Get the Response..
        	finalresult = file.getFinalResult();
		
		}else if(zType.equals("net")) {
			//Network Comms
			NET netcomm = new NET(zData, zMiniDAPPID);
			
			//Run it..
			netcomm.run();
			
			//Get the Response..
        	finalresult = netcomm.getFinalResult();
		}
	    
	    */		
	
		
		/*// we manage our particular client connection
		BufferedReader in 	 		 	= null; 
		PrintWriter out 	 			= null; 
		
		try {
			// Input Stream
			in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			
			// Output Stream
			out = new PrintWriter(mSocket.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			String firstline = new String(input);
			
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			
			// we get file requested
			String fileRequested = parse.nextToken();
			
			//Get the Headers..
			String MiniDAPPID = "0x00";
			int contentlength = 0;
			while(input != null && !input.trim().equals("")) {
				int ref = input.indexOf("Referer:"); 
				if(ref != -1) {
					//Get the referer..
					int start  = input.indexOf("/minidapps/0x");
	        		int end    = -1;
	        		if(start!=-1) {
	        			end    = input.indexOf("/", start+11);
	        		}
	        		if(end!=-1) {
	        			MiniDAPPID = input.substring(start, end);
	        		}
				}else {
					ref = input.indexOf("Content-Length:"); 
					if(ref != -1) {
						//Get it..
						int start     = input.indexOf(":");
						contentlength = Integer.parseInt(input.substring(start+1).trim());
					}
				}
					
				input = in.readLine();
			}
			
			//The final result
			String finalresult = "";
			String reqtype     = "";
			String command     = "";
			
			// Currently we support only GET
			if (method.equals("POST")){
				//Create a char buffer
				char[] cbuf = new char[contentlength];
				
				//Lets see..
				in.read(cbuf);
				
				//What is being asked..
				command = URLDecoder.decode(new String(cbuf),"UTF-8").trim();
				
				//Remove slashes..
				reqtype = new String(fileRequested);
				if(reqtype.startsWith("/")) {
					reqtype = reqtype.substring(1);
				}
				if(reqtype.endsWith("/")) {
					reqtype = reqtype.substring(0,reqtype.length()-1);
				}
				
			}else if (method.equals("GET")){
				//decode URL message
				String function = URLDecoder.decode(fileRequested,"UTF-8").trim();
				if(function.startsWith("/")) {
					function = function.substring(1);
				}
			
				if(function.startsWith("sql/")) {
					//Get the SQL function
					reqtype="sql";
					command = function.substring(4).trim();
					
				}else if(function.startsWith("file/")) {
					reqtype="file";
					command = function.substring(5).trim();
					
				}else {
					reqtype="cmd";
					command = function.trim();
				}
			
			}else {
				throw new IOException("Unsupported Method in RPCHandler : "+firstline);
			}
			
			//MinimaLogger.log("RPCHandler "+method+" "+reqtype+" "+command);
			
			//Is this a SQL function
			if(reqtype.equals("sql")) {
				//Create a SQL object
				SQL sql = new SQL(command, MiniDAPPID);
				
				//Run it..
				sql.run();
				
				//Get the Response..
            	finalresult = sql.getFinalResult();
				
			}else if(reqtype.equals("cmd")) {
				CMD cmd = new CMD(command);
            	
            	//Run it..
            	cmd.run();
 
            	//Get the Response..
            	finalresult = cmd.getFinalResult();
			
			}else if(reqtype.equals("file")) {
				//File access..
				FILE file = new FILE(command, MiniDAPPID);
				
				//Run it..
				file.run();
				
				//Get the Response..
            	finalresult = file.getFinalResult();
			
			}else if(reqtype.equals("net")) {
				//Network Comms
				NET netcomm = new NET(command, MiniDAPPID);
				
				//Run it..
				netcomm.run();
				
				//Get the Response..
            	finalresult = netcomm.getFinalResult();
			}
			
			// send HTTP Headers
			out.println("HTTP/1.1 200 OK");
			out.println("Server: HTTP RPC Server from Minima : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: text/plain");
			out.println("Content-length: " + finalresult.length());
			out.println("Access-Control-Allow-Origin: *");
			out.println(); // blank line between headers and content, very important !
			out.println(finalresult);
			out.flush(); // flush character output stream buffer
			
		} catch (Exception ioe) {
			ioe.printStackTrace();
			
		} finally {
			try {
				in.close();
				out.close();
				mSocket.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 	
		}
		
		*/
	}
}