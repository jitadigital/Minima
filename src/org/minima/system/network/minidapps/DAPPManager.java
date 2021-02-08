package org.minima.system.network.minidapps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniString;
import org.minima.system.Main;
import org.minima.system.SystemHandler;
import org.minima.system.brains.BackupManager;
import org.minima.system.input.InputHandler;
import org.minima.system.network.NetworkHandler;
import org.minima.system.network.minidapps.comms.CommsManager;
import org.minima.system.network.minidapps.minihub.hexdata.minimajs;
import org.minima.system.network.minidapps.minilib.BackEndDAPP;
import org.minima.system.network.websocket.WebSocketManager;
import org.minima.utils.Crypto;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;
import org.minima.utils.messages.Message;
import org.minima.utils.nanohttpd.protocols.http.NanoHTTPD;

public class DAPPManager extends SystemHandler {

	public static String DAPP_INIT           = "DAPP_INIT";
	public static String DAPP_INSTALL        = "DAPP_INSTALL";
	public static String DAPP_UNINSTALL      = "DAPP_UNINSTALL";
	public static String DAPP_POST           = "DAPP_POST";
	public static String DAPP_MINIDAPP_POST  = "DAPP_MINIDAPP_POST";
	
	JSONArray CURRENT_MINIDAPPS = new JSONArray();
	String MINIDAPPS_FOLDER     = "";
	
	//The MiniDAPP app server
	DAPPServer mDAPPServer;
	
	//The CommsManager for all the MIniDAPPS
	CommsManager mCommsManager; 
	
	//The Edited minima.js file..
	byte[] mMINIMAJS = new byte[0];
	
	//The old HOST..
	String mOldHost = "";
	int mBasePort   = 0;
	
	NetworkHandler mNetwork;
	
	/**
	 * BackEnd JS DAPP
	 */
	Hashtable<String, BackEndDAPP> mBackends;
	
	public DAPPManager(Main zMain) {
		super(zMain, "DAPPMAnager");
		
		//Need access to this
		mNetwork = getMainHandler().getNetworkHandler();
		
		//All the backends are stored here..
		mBackends = new Hashtable<>();
		
		//What is the current Host
		mOldHost  = mNetwork.getBaseHost();
		mBasePort = mNetwork.getBasePort();
		
		//Init the System
		PostMessage(DAPP_INIT);
	}
	
	public CommsManager getCommsManager() {
		return mCommsManager;
	}

	
	public void recalculateMinimaJS() {
		//Now create the Minima JS file..
	    try {
			//Get the bytes..
	    	byte[] minima = minimajs.returnData();
		
	    	//create a string..
	    	String minstring = new String(minima, Charset.forName("UTF-8"));
	    	
	    	//What is the RPC address
	    	String rpcaddress = mOldHost+":"+mNetwork.getRPCPort();
	    	
	    	//Now replace the RPC connect address..
		    String editstring = minstring.replace("127.0.0.1:9002",rpcaddress);
	 
		    //What is the WebSocket address
	    	String wsaddress = mOldHost+":"+mNetwork.getWSPort();
	    	
		    //Replace the Web Socket Server IP..
		    editstring = editstring.replace("127.0.0.1:9003",wsaddress);
			
		    //Now convert to bytes..
		    mMINIMAJS = editstring.getBytes();
	    
		    //MinimaLogger.log(editstring);
		    
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte[] getMinimaJS() {
		//Check if the Host has changed..
		String host = mNetwork.calculateHostIP();
		if(!host.equals(mOldHost)) {
			MinimaLogger.log("MINIDAPP RPCHOST CHANGED from "+mOldHost+" to "+host);
			mOldHost = host;
			recalculateMinimaJS();
		}
		
		return mMINIMAJS;
	}
	
	public void stop() {
		mDAPPServer.stop();
		mCommsManager.shutdown();
		
		stopMessageProcessor();
	}
	
	public JSONArray getMiniDAPPS() {
		return CURRENT_MINIDAPPS;
	}
	
	public String getMiniDAPPSFolder() {
		return MINIDAPPS_FOLDER;
	}
	
	private JSONObject loadConfFile(File zConf) {
		JSONObject ret = new JSONObject();
		
		try {
			StringBuilder tot = new StringBuilder();
			
			FileInputStream fis     = new FileInputStream(zConf);
			InputStreamReader is    = new InputStreamReader(fis);
			BufferedReader bis      = new BufferedReader(is);
			
			String sCurrentLine;
	        while ((sCurrentLine = bis.readLine()) != null) {
	        	tot.append(sCurrentLine).append("\n");
	        }
	        
	        //Now convert..
	        JSONParser parser = new JSONParser();
	        ret = (JSONObject) parser.parse(tot.toString());
	        
	        //And add the root folder..
	        String root = zConf.getParent();
	        int start = root.indexOf("/minidapps/");
	        String webroot = root.substring(start);
	        
	        String approot = root.substring(start+11);
	        int firstfolder = approot.indexOf("/");
	        if(firstfolder != -1) {
	        	approot = approot.substring(0,firstfolder);
	        }
	        
	        ret.put("uid", approot);
	        ret.put("root", webroot);
	        ret.put("web", "http://"+mNetwork.getBaseHost()+":"+mNetwork.getMiniDAPPServerPort()+webroot);
	        
	        bis.close();
	        fis.close();
	        
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	private JSONArray recalculateMiniDAPPS() {
		//Clear the OLD
		CURRENT_MINIDAPPS.clear();
		
		//And the backends..
		Enumeration<BackEndDAPP> bends = mBackends.elements();
		while(bends.hasMoreElements()) {
			BackEndDAPP bend = bends.nextElement();
			bend.shutdown();
		}
		mBackends.clear();
		
		//This is the folder..
		File alldapps = getMainHandler().getBackupManager().getMiniDAPPFolder();
		
		//Store for later
		MINIDAPPS_FOLDER = alldapps.getAbsolutePath();
		
		//List it..
		File[] apps = alldapps.listFiles();
		
		//Cycle through them..
		if(apps != null) {
			for(File app : apps) {
				//Get the time stamp - when was it installed..
				long timestamp = System.currentTimeMillis();
				File tsfile = new File(app,"minimatimestamp");
				if(tsfile.exists()) {
					try {
						FileInputStream  tsfis = new FileInputStream(tsfile);
						DataInputStream dais = new DataInputStream(tsfis);
						timestamp = dais.readLong();
						dais.close();
						tsfis.close();
					} catch (IOException e) {
						MinimaLogger.log("Error loading timestamp file.. "+e);
					}
				}
				
				//What MiniDAPP is this..
				String minidappid = app.getName();
				
				//Open it up..
				File conf    = new File(app,"minidapp.conf");
				File backend = new File(app,"backend.js");
				
				//Does it exist..
				if(!conf.exists()) {
					//Could be 1 folder down..
					File[] subapps = app.listFiles();
		
					//Has to be the first file
					if(subapps != null) {
						for(File subapp : subapps) {
							//Ignore the SQL folder that we generate..
							if(subapp.isDirectory()) {
								conf    = new File(subapp,"minidapp.conf");
								backend = new File(subapp,"backend.js");
								
								if(conf.exists()) {
									break;	
								}
							}
						}
					}
				}
				
				//minidapps install:/home/spartacusrex/git/MiFi/www/minidapps/experimental/chatter.minidapp
				
				//Check it exists..
				if(conf.exists()) {
					//Load it..
					JSONObject confjson = loadConfFile(conf);
					
					//Add the timestamp..
					confjson.put("installed", timestamp);
					
					//Is there a Back end
					if(backend.exists()) {
						//Load it..
						try {
							//Load the JS file..
							String backjs = new String(MiniFile.readCompleteFile(backend),"UTF-8");
						
							//Create a BackEnd APP..
							BackEndDAPP bedapp = new BackEndDAPP(backjs, minidappid);
							
							//Add to the List..
							mBackends.put(minidappid, bedapp);
						
							MinimaLogger.log("BackEND create for "+minidappid);
							
						} catch (Exception e) {
							MinimaLogger.log("Error loading backend.js for "+backend.getAbsolutePath());
						} 
					}
					
					//Add it..
					CURRENT_MINIDAPPS.add(confjson);
				}
			}
		}
		
		//Order the List.. By Name..
		Collections.sort(CURRENT_MINIDAPPS, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					//In case the name is missing..
					String name1 = (String) o1.get("name");
					String name2 = (String) o2.get("name");	
					return name1.compareTo(name2);
					
				}catch(Exception exc) {
					System.out.println("Error in MiniDAPP CONF "+exc);
				}
				return 0;
			}
		});
		
		return CURRENT_MINIDAPPS;
	}
	
	@Override
	protected void processMessage(Message zMessage) throws Exception {
		
		if(zMessage.getMessageType().equals(DAPP_INIT)) {
			//Create the Comms Manager
			mCommsManager = new CommsManager(getMainHandler());
			
			//Now create the Minima JS file..
		    recalculateMinimaJS();
		    
			//Calculate the current MiniDAPPS
			recalculateMiniDAPPS();
			
			//Create the MiniDAPP server
			mDAPPServer = new DAPPServer(mNetwork.getMiniDAPPServerPort(), this);
			try {
				mDAPPServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				MinimaLogger.log("MiniDAPP server started on por "+mNetwork.getMiniDAPPServerPort());
				
			} catch (IOException e) {
				MinimaLogger.log("MiniDAPP server error "+ e.toString());
			}
			
		}else if(zMessage.getMessageType().equals(DAPP_INSTALL)) {
			//Get the Data
			MiniData data = (MiniData) zMessage.getObject("minidapp");
			
			//Do we overwrite..
			boolean overwrite = false;
			if(zMessage.exists("overwrite")){
				overwrite = zMessage.getBoolean("overwrite");
			}

			//Hash it..
			MiniData hash     = Crypto.getInstance().hashObject(data, 160);
			String minidappid = hash.to0xString();
			InputHandler.getResponseJSON(zMessage).put("uid", minidappid);
			
			//This is the folder..
			File alldapps = getMainHandler().getBackupManager().getMiniDAPPFolder();
			
			//And the actual folder...
			File dapp  = new File(alldapps,hash.to0xString());
			if(dapp.exists() && !overwrite){
				InputHandler.endResponse(zMessage, true, "MiniDAPP ALLREADY installed..");
				return;
			}
			
			//Make the DAPP folder
			dapp.mkdirs();
			
			//Make a time stamp of the time of install..
			File ts = new File(dapp,"minimatimestamp");
			if(ts.exists()) {
				ts.delete();
			}
			FileOutputStream tsfos = new FileOutputStream(ts);
			DataOutputStream daos  = new DataOutputStream(tsfos);
			daos.writeLong(System.currentTimeMillis());
			daos.close();
			tsfos.close();
			
			//Now extract the contents to that folder..
			byte[] buffer = new byte[2048];
			ByteArrayInputStream bais = new ByteArrayInputStream(data.getData());
			
			BufferedInputStream bis = new BufferedInputStream(bais);
            ZipInputStream stream   = new ZipInputStream(bis);
	        ZipEntry entry          = null;
	        
	        //Cycle through all the files..
	        while ((entry = stream.getNextEntry()) != null) {
	        	//Where does this file go
	            File filePath = new File(dapp,entry.getName());
	
	            //Check the Parent
	            File parent = filePath.getParentFile();
	            if(!parent.exists()) {
	            	parent.mkdirs();
	            }
	            
	            //Do we need to make the directory
				if(entry.isDirectory()) {
					filePath.mkdirs();	
	            }else {
					//Delete if exists..
	            	if(filePath.exists()){
	            		filePath.delete();
	            	}
	            	
	            	//read it in and pump it out
		            FileOutputStream fos     = new FileOutputStream(filePath);
		            BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
		            
	                int len;
	                while ((len = stream.read(buffer)) > 0) {
	                    bos.write(buffer, 0, len);
	                }
	                
	                //Flush the system..
	                bos.flush();
	            }
	        }
	        
	        //It's done!
			recalculateMiniDAPPS();
			
			//Now get the CONF file..
			
		
			InputHandler.endResponse(zMessage, true, "MiniDAPP installed..");
			
		}else if(zMessage.getMessageType().equals(DAPP_UNINSTALL)) {
			String minidapp = zMessage.getString("minidapp");
			InputHandler.getResponseJSON(zMessage).put("minidapp", minidapp);
			
			//UNINSTALL the DAPP
			File appfolder = new File(getMiniDAPPSFolder(),minidapp);
		
			if(!appfolder.exists()) {
				InputHandler.endResponse(zMessage, false, "MiniDAPP not found..");	
				return;
			}
			
			//Delete the app root..
			BackupManager.safeDelete(appfolder);
			
			//Recalculate the MINIDAPPS
			recalculateMiniDAPPS();
			
			InputHandler.endResponse(zMessage, true, "MiniDAPP uninstalled..");
			
		}else if(zMessage.getMessageType().equals(DAPP_POST)) {
			//Send a MinimaEvent Message to all the current Backend DAPPS
			String minidapp = zMessage.getString("minidapp");
			String message  = zMessage.getString("message");
			
			//Make a JSON
			JSONObject json = new JSONObject();
			json.put("action", "post");
			json.put("message", message);
			
			JSONObject wsmsg = new JSONObject();
			wsmsg.put("event","network");
			wsmsg.put("details",json);
			
			Message msg = new Message(DAPP_MINIDAPP_POST);
			msg.addString("minidappid", minidapp);
			msg.addObject("message", wsmsg);
			PostMessage(msg);
		
			InputHandler.getResponseJSON(zMessage).put("minidapp", minidapp);
			InputHandler.getResponseJSON(zMessage).put("message", wsmsg.toString());
			InputHandler.endResponse(zMessage, true, "Message posted");
		
		}else if(zMessage.isMessageType(DAPP_MINIDAPP_POST)) {
			//What is the message..
			JSONObject json = (JSONObject) zMessage.getObject("message");
			
			String minidappid = "";
			if(zMessage.exists("minidappid")) {
				minidappid = zMessage.getString("minidappid");
			}
			
			//First the Back End..
			sendToBackEND(minidappid,json);
			
			//Now the Front End..
			if(minidappid.equals("")) {
				Message msg = new Message(WebSocketManager.WEBSOCK_SENDTOALL);
				msg.addString("message", json.toString());
				mNetwork.getWebSocketManager().PostMessage(msg);				
			}else {
				Message msg = new Message(WebSocketManager.WEBSOCK_SEND);
				msg.addString("message", json.toString());
				msg.addString("minidappid", minidappid);
				mNetwork.getWebSocketManager().PostMessage(msg);
			}
		}
	}
	
	private void sendToBackEND(String zMiniDAPPID, JSONObject zJSON) {
		//Create the same EVent as on the Web
	    JSONObject jobj  = CreatePostEvent(zJSON);
	    String JSONEvent = jobj.toString();
	    
		if(zMiniDAPPID.equals("")){
			Enumeration<BackEndDAPP> bends = mBackends.elements();
			while(bends.hasMoreElements()) {
				BackEndDAPP bend = bends.nextElement();
				bend.MinimaEvent(JSONEvent);
			}
		}else {
			BackEndDAPP bend = mBackends.get(zMiniDAPPID);
			if(bend != null) {
				bend.MinimaEvent(JSONEvent);
			}
		}	
	}
	
	/**
	 * Make the event the same as when on the web page..
	 * 
	 * @param zEventType
	 * @param zJSON
	 */
	private JSONObject CreatePostEvent(JSONObject zJSON) {
		String event = (String) zJSON.get("event");
		
		JSONObject data = new JSONObject();
		data.put("event", event);
		
		if(event.equals("newblock")) {
			data.put("info", zJSON.get("txpow"));	
		
		}else if(event.equals("newtransaction")) {
			data.put("info", zJSON.get("txpow"));	
			
		}else if(event.equals("newbalance")) {
			data.put("info", zJSON.get("balance"));	
			
		}
		
		JSONObject evt = new JSONObject();
		evt.put("detail", data);
		
		return evt;
	}
}
