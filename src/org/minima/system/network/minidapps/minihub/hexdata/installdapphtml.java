package org.minima.system.network.minidapps.minihub.hexdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.minima.objects.base.MiniData;

public class installdapphtml {
	//FILE SIZE 647
	//HEX NUM 1
	public final static int HEXNUM = 1;
	public final static byte[][] HEXDATA = new byte[1][];
	public static byte[] FINAL_ARRAY = null;
	static {
	//0 - 647
	HEXDATA[0] = new MiniData("0x3C68746D6C3E0A0A3C686561643E0A093C6C696E6B2072656C3D227374796C6573686565742220747970653D22746578742F6373732220687265663D226373732F6D696E6964617070732E637373223E0A3C2F686561643E0A0A3C626F64793E0A0A3C73637269707420747970653D22746578742F6A617661736372697074223E0A0A092F2F57616974206120666577207365636F6E6473207468656E206A756D70206261636B2E2E0A0973657454696D656F75742866756E6374696F6E28297B2077696E646F772E6C6F636174696F6E2E687265663D27696E6465782E68746D6C273B207D2C2033303030293B0A0A3C2F7363726970743E0A0A3C63656E7465723E0A3C62723E3C62723E3C62723E3C62723E0A0A3C7461626C652077696474683D333030206865696768743D33303020636C6173733D6D61696E626F6479207374796C653D22746578742D616C69676E3A63656E7465723B666F6E742D73697A653A31383B223E0A3C74723E0A3C74643E0A0A4C4F4144494E4720444150502E2E3C62723E0A3C62723E0A0A3C646976207374796C653D22746578742D616C69676E3A63656E7465723B666F6E742D73697A653A31383B223E0A0A3C64697620636C6173733D226C6F6164657222207374796C653D22646973706C61793A20696E6C696E652D626C6F636B223E3C2F6469763E0A0A3C2F6469763E0A0A3C2F74643E0A3C2F74723E0A3C2F7461626C653E0A0A3C62723E0A3C646976207374796C653D22746578742D616C69676E3A63656E7465723B666F6E742D73697A653A31363B223E0A506C656173652077616974206120666577207365636F6E64732E2E200A3C2F6469763E0A3C2F63656E7465723E0A0A3C2F626F64793E0A0A3C2F68746D6C3E").getData();
	}

			public static byte[] returnData() throws IOException {
				if(FINAL_ARRAY == null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					for(int i=0;i<HEXNUM;i++) {
						baos.write(HEXDATA[i]);
					}
					baos.flush();
					FINAL_ARRAY = baos.toByteArray();	
				}
			    return FINAL_ARRAY;
			}
}
