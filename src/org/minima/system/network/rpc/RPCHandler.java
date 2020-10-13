package org.minima.system.network.rpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.StringTokenizer;

import org.minima.system.backup.BackupManager;
import org.minima.system.input.InputHandler;
import org.minima.system.network.commands.CMD;
import org.minima.system.network.commands.SQL;
import org.minima.utils.MinimaLogger;
import org.minima.utils.SQLHandler;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

/**
 * This class handles a single request then exits
 * 
 * @author spartacusrex
 *
 */
public class RPCHandler implements Runnable {

	/**
	 * The Net Socket
	 */
	Socket mSocket;
	
	/**
	 * Main COnstructor
	 * @param zSocket
	 */
	public RPCHandler(Socket zSocket) {
		//Store..
		mSocket = zSocket;
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in 	 		 	= null; 
		PrintWriter out 	 			= null; 
		
		String fileRequested 			= null;
		
		try {
			// Input Stream
			in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			
			// Output Stream
			out = new PrintWriter(mSocket.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			
			// we get file requested
			fileRequested = parse.nextToken();
			
			//Get the Headers..
			String MiniDAPPID = "";
			int contentlength = 0;
			while(input != null && !input.trim().equals("")) {
//				System.out.println("line : "+input);
				int ref = input.indexOf("Referer:"); 
				if(ref != -1) {
					//Get the referer..
					int start  = input.indexOf("/minidapps/0x")+11;
	        		int end    = -1;
	        		if(start!=-1) {
	        			end    = input.indexOf("/", start);
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
			
			//CMD, SQL, FILE
			String type        = "";
			
			//Request
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
				type = new String(fileRequested);
				if(type.startsWith("/")) {
					type = type.substring(1);
				}
				if(type.endsWith("/")) {
					type = type.substring(0,type.length()-1);
				}
				
			}else if (method.equals("GET")){
				//decode URL message
				String function = URLDecoder.decode(fileRequested,"UTF-8").trim();
				if(function.startsWith("/")) {
					function = function.substring(1);
				}
			
				if(function.startsWith("sql/")) {
					//Get the SQL function
					type="sql";
					command = function.substring(4).trim();
				}else {
					type="cmd";
					command = function.trim();
				}
			
			}else {
				MinimaLogger.log("Unsupported Method in RPCHandler : "+method);
				return;
			}
			
			MinimaLogger.log("RPCHandler "+type+" "+command);
			
			
			//Is this a SQL function
			if(type.equals("sql")) {
				//Create a SQL object
				SQL sql = new SQL(command, MiniDAPPID);
				
				//Run it..
				sql.run();
				
				//Get the Response..
            	finalresult = sql.getFinalResult();
				
			}else if(type.equals("cmd")) {
				CMD cmd = new CMD(command);
            	
            	//Run it..
            	cmd.run();
 
            	//Get the Response..
            	finalresult = cmd.getFinalResult();
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
	}
}
