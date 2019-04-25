/**
 * 
 */
package com.virtress.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.virtress.assets.Asset;
import com.virtress.assets.AssetLoader;
import com.virtress.assets.Group;
import com.virtress.assets.Header;
import com.virtress.common.HttpRequestMethod;

/**
 * @author ThisIsDef
 *
 */
public class ServerStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean serverOn = true;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(2800);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Unable to create server socket.");
		}
		
		while (serverOn) {
			try (Socket socket = serverSocket.accept()) {
			    socket.setKeepAlive(true);
			    socket.setSoLinger(true, 10000);
				socket.setSoTimeout(10000);
				InputStreamReader isr = new InputStreamReader(socket.getInputStream());
				BufferedReader reader = new BufferedReader(isr);
				List<String> requestHeaders = new ArrayList<>();
				boolean extractBody = false;
				int bodyLength = 0;
				String urlPath = "";
				StringBuffer buffer = new StringBuffer();
				String line = "";
				String httpMethod = "";
				while ((line = reader.readLine()) != null && !line.isEmpty()) {
					System.out.println(line);
					requestHeaders.add(line);
					buffer.append(line + "\r\n");
					for (HttpRequestMethod method : HttpRequestMethod.values()) {
						if (line.startsWith(method.name())) {
							urlPath = line.split(" ")[1];
						}
					}
					String[] spacedArray = line.split(" ");
					if (spacedArray.length > 0) {
						if (HttpRequestMethod.contains(spacedArray[0])) {
							httpMethod = spacedArray[0];
						}
					}
					if (line.startsWith(HttpRequestMethod.POST.name())) {
			            extractBody = true;
			        }
			        if (line.toLowerCase().startsWith("content-length:")) {
			            bodyLength = Integer.valueOf(line.substring(line.indexOf(' ') + 1));
			        }
				}
				
				String requestHeader = buffer.toString();
			    String requestBody = "";
		
			    if (extractBody) {
			        char[] body = new char[bodyLength];
			        reader.read(body, 0, bodyLength);
			        requestBody = new String(body);
			    }
			    
			    System.out.println(requestHeader);
			    System.out.println(requestBody);
			    
			    // Send Response
			    AssetLoader assetLoader = new AssetLoader();
			    List<Asset> allAssets = assetLoader.loadAssets();
			    String assetResponse = "";
			    Group matchedGroup = null;
			    for (Asset asset : allAssets) {
			    	matchedGroup = asset.getGroup(urlPath, requestHeaders, httpMethod);
			    	if (matchedGroup != null) {
			    		System.out.println("Setting assetResponse to matcher response: " + matchedGroup.getResponse());
			    		assetResponse = matchedGroup.getResponse();
			    		// We found the matching asset response. No need to continue.
			    		break;
			    	}
			    }
			    SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:Ss z");
			    String res = "HTTP/1.0 200 OK\n"
			            + "Server: HTTP server/0.1\n"
			            + "Date: "+format.format(new java.util.Date())+"\n"
			      + "Content-type: " + (matchedGroup != null ? matchedGroup.getContentType() : "text/html")  + "; charset=UTF-8\n"
			            + "Content-Length: " + (assetResponse.isEmpty() ? "0" : assetResponse.length()) + "\n";
			    if (matchedGroup.getResponseHeaders() != null) {
			    	for (Header responseHeader : matchedGroup.getResponseHeaders()) {
			    		res += responseHeader.getName() + ": " + responseHeader.getValue() + "\n";
			    	}
			    }
			    res += "\n"; // Separates the response headers from the body.
			    res += (assetResponse.isEmpty() ? "{ \"status\":\"Asset not found\"" : assetResponse);
			    BufferedWriter out = new BufferedWriter(
			    	    new OutputStreamWriter(
			    	        new BufferedOutputStream(socket.getOutputStream()), "UTF-8"));
			    out.write(res);
			    out.flush();
			    out.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("There was an error while extracting the body.");
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("An unknown exception happened.");
			}
		}
	}

}
