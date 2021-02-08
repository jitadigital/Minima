package org.minima.system.network.minidapps;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.minima.objects.base.MiniData;
import org.minima.system.network.minidapps.minihub.hexdata.faviconico;
import org.minima.system.network.minidapps.minihub.hexdata.helphtml;
import org.minima.system.network.minidapps.minihub.hexdata.iconpng;
import org.minima.system.network.minidapps.minihub.hexdata.indexhtml;
import org.minima.system.network.minidapps.minihub.hexdata.installdapphtml;
import org.minima.system.network.minidapps.minihub.hexdata.minidappscss;
import org.minima.system.network.minidapps.minihub.hexdata.tilegreyjpeg;
import org.minima.system.network.minidapps.minihub.hexdata.uninstalldapphtml;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.nanohttpd.protocols.http.IHTTPSession;
import org.minima.utils.nanohttpd.protocols.http.NanoHTTPD;
import org.minima.utils.nanohttpd.protocols.http.request.Method;
import org.minima.utils.nanohttpd.protocols.http.response.Response;
import org.minima.utils.nanohttpd.protocols.http.response.Status;

public class DAPPServer extends NanoHTTPD{

	DAPPManager mDAPPManager;

	/**
	 * Store the current page and MiniDAPP list..
	 * So you don't recreate it EVERY time..
	 */
	String mCurrentIndex     = "**";
	String mCurrentMiniDAPPS = "**";
	
	String mTestWeb;
	
	
	public DAPPServer(int zPort, DAPPManager zDAPPManager) {
		super(zPort);
		
		mDAPPManager = zDAPPManager;
		
		//The test web folder..
		mTestWeb = zDAPPManager.getMainHandler().getBackupManager().getTestWebFolder().getAbsolutePath();
	}

	@Override
    public Response serve(IHTTPSession session) {
        try {
        	//GET or POST
        	Method method = session.getMethod();
			
        	//What are they looking for..
        	String fileRequested = session.getUri();
        	
        	//Which MiniDAPP
        	String MiniDAPPID="";
        	String ref = session.getHeaders().get("referer");
        	if(ref != null) {
        		int start  = ref.indexOf("/minidapps/");
        		int end    = -1;
        		if(start!=-1) {
        			end    = ref.indexOf("/", start+11);
        		}
        		
        		if(end!=-1) {
        			MiniDAPPID = ref.substring(start, end);
        		}
        	}

//        	if(MiniDAPPID.equals("")) {;
//        		MinimaLogger.log("UNKNOWN MINIDAPP! file:"+fileRequested+" headers:"+session.getHeaders());
//        	}
        	
        	//Quick clean
			if(fileRequested.endsWith("/")) {
				fileRequested = fileRequested.concat("index.html");
			}
			if(fileRequested.startsWith("/")) {
				fileRequested = fileRequested.substring(1);
			}
        	
			//GET or POST
			if(Method.GET.equals(method)) {
				//Any parameters
	        	Map<String, List<String>> params = session.getParameters();
	        	
	        	//Is there an uninstall..
	        	String uninst = "";
	        	List<String> uninstall = params.get("uninstall");
	        	if(uninstall != null) {
	        		uninst = uninstall.get(0);
	        	}
	        	
		        //Are we uninstalling a MiniDAPP
				if(fileRequested.equals("uninstalldapp.html")) {
					if(!uninst.equals("")) {
						//POST it..
						Message msg = new Message(DAPPManager.DAPP_UNINSTALL);
						msg.addObject("minidapp", uninst);
						mDAPPManager.PostMessage(msg);
			            
		                return getOKResponse(uninstalldapphtml.returnData(), "text/html");
		                
//						//UNINSTALL the DAPP
//						File appfolder = new File(mDAPPManager.getMiniDAPPSFolder(),uninst);
//					
//						//Delete the app root..
//						BackupManager.safeDelete(appfolder);
//						
//						//Recalculate the MINIDAPPS
//						mDAPPManager.recalculateMiniDAPPS();
//						
//						//Return the main index page..
//						String page    = new String(indexhtml.returnData(),StandardCharsets.UTF_8);
//						String newpage = page.replace("######", createMiniDAPPList());
//						return getOKResponse(newpage.getBytes(), "text/html");
					}else {
						fileRequested = "index.html";
					}
				}
			
				//Otherwise lets see..
				if(fileRequested.startsWith("testweb/")) {
					//get from the testweb folder..
					String fullfile = mTestWeb+"/"+fileRequested.substring(8);
					byte[] file     = MiniFile.readCompleteFile(new File(fullfile));
					
					if(file.length>0) {
						return getOKResponse(file, MiniFile.getContentType(fullfile));
					}else {
						return getNotFoundResponse();
					}
					
				}else if(fileRequested.endsWith("/minima.js") || fileRequested.equals("minima.js")) {
					return getOKResponse(mDAPPManager.getMinimaJS() , "text/javascript");
				
				}else if(fileRequested.startsWith("minidapps/")) {
					//Send the MiniDAPP!
					String fullfile = mDAPPManager.getMiniDAPPSFolder()+"/"+fileRequested.substring(10);
					byte[] file     = MiniFile.readCompleteFile(new File(fullfile));
					
					if(file.length>0) {
						return getOKResponse(file, MiniFile.getContentType(fullfile));
					}else {
						return getNotFoundResponse();
					}
					
				}else {
					if(fileRequested.equals("index.html")) {
						String page    = new String(indexhtml.returnData(),StandardCharsets.UTF_8);
						String newpage = page.replace("######", createMiniDAPPList());
						return getOKResponse(newpage.getBytes(), "text/html");
						
					}else if(fileRequested.equals("css/minidapps.css")) {
						return getOKResponse(minidappscss.returnData(), "text/css");
						
					}else if(fileRequested.equals("favicon.ico")) {
						return getOKResponse(faviconico.returnData(), "image/ico");
						
					}else if(fileRequested.equals("help.html")) {
						return getOKResponse(helphtml.returnData(), "text/html");
					
					}else if(fileRequested.equals("icon.png")) {
						return getOKResponse(iconpng.returnData(), "image/png");
						
					}else if(fileRequested.equals("installdapp.html")) {
						return getOKResponse(installdapphtml.returnData(), "text/html");
						
					}else if(fileRequested.equals("tile-grey.jpeg")) {
						return getOKResponse(tilegreyjpeg.returnData(), "image/jpeg");
						
					}else {
						//Not found..
						return getNotFoundResponse();	
					}	
				}
			
			}else if(Method.POST.equals(method)) {
				//Only on the Install DAPP page..
				if(fileRequested.equals("installdapp.html")) {
					//get the file..
			        Map<String, String> files = new HashMap<String, String>();
		            session.parseBody(files);
	            
		            //Get the File..
		            String minidappfile = files.get("minidapp");
		            
		            //Load the file..
		            byte[] file = MiniFile.readCompleteFile(new File(minidappfile));
					
		            //Create a MiniData Object..
		            MiniData dapp = new MiniData(file);
					
					//POST it..
					Message msg = new Message(DAPPManager.DAPP_INSTALL);
					msg.addObject("minidapp", dapp);
					mDAPPManager.PostMessage(msg);
		            
	                return getOKResponse(installdapphtml.returnData(), "text/html");
	                
				}else if(fileRequested.startsWith("minidapps/")) {
					String fullfile = mDAPPManager.getMiniDAPPSFolder()+"/"+fileRequested.substring(10);
					byte[] file     = MiniFile.readCompleteFile(new File(fullfile));
					
					if(file.length>0) {
						return getOKResponse(file, MiniFile.getContentType(fullfile));
					}else {
						return getNotFoundResponse();
					}
				}else if(fileRequested.startsWith("testweb/")) {
					//get from the testweb folder..
					String fullfile = mTestWeb+"/"+fileRequested.substring(8);
					byte[] file     = MiniFile.readCompleteFile(new File(fullfile));
					
					if(file.length>0) {
						return getOKResponse(file, MiniFile.getContentType(fullfile));
					}else {
						return getNotFoundResponse();
					}
					
				}
				
			}
        	
			return getNotFoundResponse();
     
        } catch (Exception ioe) {
        	MinimaLogger.log("DAPPSERVER Error : "+ioe);
        	
        	return getInternalErrorResponse("INTERNAL ERROR");
        }
    }
	
	protected Response getOKResponse(byte[] zHTML, String zContentType) {
		Response resp = Response.newFixedLengthResponse(Status.OK, zContentType, zHTML);
		resp.addHeader("Server", "HTTP RPC Server from Minima v0.95.14");
		resp.addHeader("Date", new Date().toString());
		
		//Cache images..
		if(zContentType.startsWith("image/")) {
			resp.addHeader("Cache-Control", "max-age=86400");
		}
		
//		resp.addHeader("Access-Control-Allow-Origin", "*");
		
        return resp;
    }
	
	protected Response getForbiddenResponse(String s) {
        return Response.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse() {
        return Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }
	
    
    public String createMiniDAPPList() throws Exception {
		//get the current MIniDAPPS
		JSONArray alldapps = mDAPPManager.getMiniDAPPS();
		
		//Check if there is a change
		String alldappstr = alldapps.toString();
		if(alldappstr.equals(mCurrentMiniDAPPS)) {
			return mCurrentIndex;
		}
		
		//Store it..
		mCurrentMiniDAPPS = alldappstr;
		
		//Build it..
		StringBuilder list = new StringBuilder();
		list.append("<table width=100%>");
		
		int len = alldapps.size();
		for(int i=0;i<len;i++) {
			JSONObject app = (JSONObject) alldapps.get(i);
			
			//Now do it..
			String root  = (String) app.get("root");
			String uid  = (String) app.get("uid");
			String name  = (String) app.get("name");
			String desc  = (String) app.get("description");
			String backg = root+"/"+(String) app.get("background");
			String icon  = root+"/"+(String) app.get("icon");
			String webpage  = root+"/index.html";
			
			String openpage = "_"+name;
	
			String date = MinimaLogger.DATEFORMAT.format(new Date((Long)app.get("installed"))); 
			String version = uid+" @ "+date;
			
			//Now do it..
			list.append("<tr><td>" + 
					"			<table style='background-size:100%;background-image: url("+backg+");' width=100% height=100 class=minidapp>" + 
					"			 	<tr>" + 
					"					<td style='cursor:pointer;' rowspan=2 onclick=\"window.open('"+webpage+"', '"+openpage+"');\">" + 
					"						<img src='"+icon+"' height=100>" + 
					"					</td>" + 
					"					<td width=100% class='minidappdescription'>" + 
					"                   <div style='position:relative'>" + 
					"				        <div onclick='uninstallDAPP(\""+name+"\",\""+uid+"\");' style='color:red;cursor:pointer;position:absolute;right:10;top:10'>UNINSTALL</div>" + 
					"						<br><div onclick=\"window.open('"+webpage+"','"+openpage+"');\" style='cursor:pointer;font-size:18'><b>"+name.toUpperCase()+"</b></div>" + 
					"						<br><div onclick=\"window.open('"+webpage+"','"+openpage+"');\" style='cursor:pointer;font-size:12;min-height:20;'>"+desc+"</div>" + 
					"						<div onclick=\"window.open('"+webpage+"','"+openpage+"');\" style='cursor:pointer;color:blue;font-size:10;text-align:right;width:98%;'><br>"+version+"</div>"+
					"					</div>"+
					"                     </td>" + 
					"				</tr>" + 
					"			</table>" + 
					"		</td></tr>");
		}
		
		if(len == 0) {
			list.append("<tr><td style='text-align:center;'><br><br><b>NO DAPPS INSTALLED YET..</b>"
					+ "<br><br><br>"
					+ "Go to <a href='http://mifi.minima.global/' target='_blank'>http://mifi.minima.global/</a> to find MiniDAPPs"
					+ "</td></tr>");
		}else {
//			list.append("<tr><td style='text-align:center;'><br><br>"
//					+ "Go to <a href='http://mifi.minima.global/' target='_blank'>http://mifi.minima.global/</a> to find MiniDAPPs"
//					+ "</td></tr>");
		}
		
		list.append("</table>");
		
		//Store A copy..
		mCurrentIndex = list.toString();
		
		return mCurrentIndex;
	}
	
}
