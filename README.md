# mini-file-server

_Local java-based file server for SimWrapper_

Run this .jar in a local folder on your machine to provide a simple HTTP file server, 
perfect for serving up files for [SimWrapper](https://simwrapper.github.io).

Usage:

`java -jar mini-file-server.jar [root folder]` 

- Mini file server will serve all files and subfolders in the root folder that you specify. 
- If no root is specified, it will serve everything in the working directory from which it is started.
- Requires Jave JRE 11 or above

