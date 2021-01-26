/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of Mini Wegb Server / SimpleWebServer.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ServerSideScriptEngine.java,v 1.4 2004/02/01 13:37:35 pjm2 Exp $

*/

package org.jibble.simplewebserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright Paul Mutton
 * http://www.jibble.org/
 *
 */
public class RequestThread extends Thread {
    
    public RequestThread(Socket socket, File rootDir) {
        _socket = socket;
        _rootDir = rootDir;
    }
    
    private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException {
        out.write(("HTTP/1.0 " + code + " OK\r\n" + 
                   "Date: " + new Date().toString() + "\r\n" +
                   "Server: JibbleWebServer/1.0\r\n" +
                   "Content-Type: " + contentType + "\r\n" +
                   "Expires: Thu, 01 Dec 1971 16:00:00 GMT\r\n" +

                   "Access-Control-Allow-Origin: *\r\n" +
                   "Cache-Control: no-cache, max-age=0, must-revalidate, no-store\r\n" +

                   ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
                   "Last-modified: " + new Date(lastModified).toString() + "\r\n" +
                   "\r\n").getBytes());
    }
    
    private static void sendError(BufferedOutputStream out, int code, String message) throws IOException {
        message = message + "<hr>" + SimpleWebServer.VERSION;
        sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
        out.write(message.getBytes());
        out.flush();
        out.close();
    }
    
    public void run() {
        InputStream reader = null;
        try {
            _socket.setSoTimeout(30000);
            BufferedReader in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(_socket.getOutputStream());
            
            String request = in.readLine();
            if (request == null || !request.startsWith("GET ") || !(request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                // Invalid request type (no "GET")
                sendError(out, 500, "Invalid Method.");
                return;
            }            
            String path = request.substring(4, request.length() - 9);            
            File file = new File(_rootDir, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();

            System.out.println(path);

            if (file.isDirectory()) {
                // Check to see if there is an index file in the directory.
                File indexFile = new File(file, "index.html");
                if (indexFile.exists() && !indexFile.isDirectory()) {
                    file = indexFile;
                }
            }

            if (!file.toString().startsWith(_rootDir.toString())) {
                // Uh-oh, it looks like some lamer is trying to take a peek
                // outside of our web root directory.
                sendError(out, 403, "Permission Denied.");
            }
            else if (!file.exists()) {
                // The file was not found.
                sendError(out, 404, "File Not Found.");
            }
            else if (file.isDirectory()) {
                // print directory listing
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                File[] files = file.listFiles();
                Arrays.sort(files);
                sendHeader(out, 200, "text/html", -1, System.currentTimeMillis());
                String title = "Index of " + path;
                out.write(("<html><head><title tag=\"SimpleWebServer\">" + title + "</title></head>" +
                        "<body style='background-color: #fea; font-size: 18px; line-height: 1.5rem; font-family: monospace;'>" +
                        "<h2 style='margin-top: 1.5rem;'>Index of " + path + "</h2><hr>" +
                        "<ul>\n").getBytes());

                for (int i = 0; i < files.length; i++) {
                    file = files[i];
                    String filename = file.getName();
                    if (file.isDirectory()) filename += "/";
                    out.write(("<li><a href=\"" + path + filename + "\">" + filename + "</a></li>\n").getBytes());
                }
                out.write(("</ul><hr><p>" + SimpleWebServer.VERSION + "</p></body><html>").getBytes());
            }
            else {
                reader = new BufferedInputStream(new FileInputStream(file));
            
                String contentType = (String)SimpleWebServer.MIME_TYPES.get(SimpleWebServer.getExtension(file));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                sendHeader(out, 200, contentType, file.length(), file.lastModified());
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                reader.close();
            }
            out.flush();
            out.close();
        }
        catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception anye) {
                    // Do nothing.
                }
            }
        }
    }
    
    private File _rootDir;
    private Socket _socket;
    
}