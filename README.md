# [Presentation Live Feedback](https://github.com/Skyball2000/presentation-live-feedback)

A small java application hosting a web server.  
Allows list of users to show reactions and raise their hands. Using an admin password provided by the application, the
host can send messages to the users and reset their reactions/hand status.

![Screenshot of the application with two users as admin](doc/application-screenshot.png)

Admin view of the application.

## Setup

Build the application using maven:

```bash
mvn clean package
```

and run it using at least Java 11.

Command line arguments:

- `ws` `webSocketPort`: port to use for the web socket server. Default is 8081
- `hs` `httpPort`: port to use for the http server. Default is 8080
- `pw` `password`: password to use for the admin. Default is a random alphanumeric string
- `ctx` `context` `path`: path of the http server. Default is `/`

In the running application process:

- use `password` to print the admin password
- use `open` to open the application using localhost in the browser
- use `exit` to exit the application

Access the web interface at http://localhost:8080/ or your specified port/path respectively.  
If you want to be able to access the web interface from another machine, simply open both ports in your firewall and use
the host's IP address/DNS name.
