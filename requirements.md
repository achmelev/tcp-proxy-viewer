#Project goals

The goal is to build an application which intercerpts TCP connections and shows the user it's  conntents. The general usage idea:

After starting the app the user is presented with a simple GUI consisting of a menu and two panes. With File->Start the user gets a listenet dialog with the following fields:

- Local IP, prefilled with 127.0.0.1
- Local Port
- Target IP or host name
- Target port

After filling out and clicking OK, the listenung session starts. In the left pane the user sees a list of the accepted TCP connections. After selecting one of them, the user sees in the right pane the contents of the selected connection, presented like a chat converation between the client and the server.

#Tech Stack

##Programming language

Use Java

##Frameworks

Use spring boot.

##Tools

Use maven as build tool. 