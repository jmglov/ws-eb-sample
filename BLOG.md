In this blog post, we'll show how to run a Clojure websocket server on AWS
[Elastic Beanstalk] [1]. We'll use [HTTP Kit] [2] (a [Ring] [3]-compatible
HTTP and websocket server), [Compojure] [4] for routing, and [Docker] [5] to
deploy our server to Elastic Beanstalk.

## Building a basic websocket server in Clojure

Create a new Leiningen project by running in a terminal:

    lein new app ws-eb-sample
    cd ws-eb-sample

Edit the `project.clj` file to look like this:

    (defproject ws-eb-sample "0.1.0-SNAPSHOT"
      :description "Sample websocket server on AWS Elastic Beanstalk"
      :dependencies [[org.clojure/clojure "1.6.0"]
                     [compojure "1.3.0"]
                     [http-kit "2.1.16"]
                     [javax.servlet/servlet-api "2.5"]]
    
      :main ws-eb-sample.core
    
      :profiles {:uberjar {:aot :all}})

Now, we can write a **really** basic server. Edit the `src/ws_eb_sample/core.clj`
file to look like this:

    (ns ws-eb-sample.core
      (:require [compojure.core :refer [defroutes GET]]
                [compojure.handler :as handler]
                [org.httpkit.server :as ws])
      (:gen-class))
    
    (defn handle-websocket [req]
      (ws/with-channel req con
        (println "Connection from" con)
    
        (ws/on-receive con #(ws/send! con (str "You said: " %)))
    
        (ws/on-close con (fn [status]
                           (println con "disconnected with status" (name status))))))
    
    (defroutes routes
      (GET "/ws" [] handle-websocket))
    
    (def application (handler/site routes))
    
    (defn -main [& _]
      (let [port (-> (System/getenv "SERVER_PORT")
                     (or "8080")
                     (Integer/parseInt))]
        (ws/run-server application {:port port, :join? false})
        (println "Listening for connections on port" port)))

Now, let's fire up the server! From the project's directory, run `lein run`.
In your terminal, you should see:

   Listening for connections on port 8080

Now, grab yourself a websocket client. I use the excellent [Simple WebSocket Client] [6]
Chrome extension, which I'll assume you're using for the rest of this section, but it
should be very easy to follow along with any websocket client of your choosing.

Start Simple Websocket Client, enter "ws://localhost:8080/ws" as the URL in the
Server Location section, then click the **Open** button. Now, in the Request textarea,
type a message like "Hello, world!", and click the **Send** button. In the Message
Log section, you should see your "Hello, world!" message, followed almost immediately
by the server's "You said: Hello, world!" response. Go ahead and click the **Close**
button up in the Server Location section, and head back to your terminal. You should see
something like:

    Listening for connections on port 8080
    Connection from #<AsyncChannel /0:0:0:0:0:0:0:1:8080<->/0:0:0:0:0:0:0:1:47874>
    #<AsyncChannel /0:0:0:0:0:0:0:1:8080<->/0:0:0:0:0:0:0:1:47874> disconnected with status normal

OK, you now have a working websocket server!

You can see the entire sample project on Github:
[https://github.com/jmglov/ws-eb-sample](https://github.com/jmglov/ws-eb-sample)

## Running in Docker

If you haven't already, go ahead and [install Docker] [7]. When you've got it
installed and running, let's create an image for our server! Go back to your
terminal in the project directory and type:

    lein clean && lein uberjar

If everything went well, you should see something like this:

    Compiling ws-eb-sample.core
    Created /home/jmglov/ws-eb-sample/target/ws-eb-sample-0.1.0-SNAPSHOT.jar
    Created /home/jmglov/ws-eb-sample/target/ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar

The `ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar` file is the so-called uberjar,
which is a runnable jar containing our server and all of its dependencies. Let's
try it out by typing:

    cd target/
    java -jar ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar

You should see the startup message and be able to connect to the server with your
websocket client on ws://localhost:8080/ws as before.

Now, we'll create a file called `Dockerfile` in the `target` directory, with the
following contents:

    FROM java:7
    ADD ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar ws-eb-sample.jar
    EXPOSE 80
    CMD ["/usr/bin/java","-jar","/ws-eb-sample.jar"]

We can build a Docker image by running:

    tar czf context.tar.gz Dockerfile ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar
    docker build -t 'clojure/ws-eb-sample:0.0.1-SNAPSHOT' - <context.tar.gz

You'll see something like this:

    Sending build context to Docker daemon 5.678 MB
    Sending build context to Docker daemon 
    Step 0 : FROM java:7
     ---> 2711b1d6f3aa
    Step 1 : ADD ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar ws-eb-sample.jar
     ---> a3f7c7532dc4
    Removing intermediate container a26dc08e4bcd
    Step 2 : EXPOSE 80
     ---> Running in 8d5d20438db4
     ---> f2ae5a747885
    Removing intermediate container 8d5d20438db4
    Step 3 : CMD /usr/bin/java -jar /ws-eb-sample.jar
     ---> Running in 1cf4873b8611
     ---> d8deb4f7eb7a
    Removing intermediate container 1cf4873b8611
    Successfully built d8deb4f7eb7a

Now, you can run the Docker image in a new container:

    docker run -p 8080:8080 'clojure/ws-eb-sample:0.0.1-SNAPSHOT'

Again, you should see the startup message and be able to connect to the server on
ws://localhost:8080/ws .

## Creating a Dockerisable file for Elastic Beanstalk

OK, so "Dockerisable" is not really a word, but I didn't want to type "a file
containing the uberjar and Dockerfile from which Elastic Beanstalk will be able to
build a Docker image". Which I've now typed, but I like my new word so much I'm
leaving it in.

Still in the `target` directory, run:

   zip ws-eb-sample.zip Dockerfile ws-eb-sample-0.1.0-SNAPSHOT-standalone.jar

And now we've got something that Elastic Beanstalk can Dockerise!

## Deploying to Elastic Beanstalk

First, let's create an Elastic Beanstalk application and environment:

1. From the [Elastic Beanstalk console] [7], click the **Create New Application**
   button.
1. On the Application Information page, give your application and name, then click
   the **Next** button.
1. On the Environment Type page, select **Web Server** as your environment tier,
   **Docker** as your predefined configuration, and **Load balancing, autoscaling**
   as your environment type, then click the **Next** button.
1. On the Application Version page, select **Upload your own**, then click the
   **Choose file** button and select the `ws-eb-sample.zip` file that you prepared
   previously. The default deployment settings are fine, so click the **Next**
   button. The file you selected will be uploaded before the console proceeds to
   the next page, which may take a little while, depending on the uplink speed of
   your Internet connection.
1. On the Environment Information page, enter something like "sample" as your
   environment name, then pick something unique as your environment URL. It's
   probably a good idea to click the **Check availability** button to ensure that
   you can get the URL before proceeding. Click the **Next** button once you've
   chosen a unique URL.
1. On the Additional Resources page, simply click the **Next** button.
1. On the Configuration Details page, you can just accept the defaults and click
   the **Next** button.
1. On the Environment Tags page, click the **Next** button.
1. On the Review page, click the **Launch** button.

Wait for the environment to be created and launched. This will take a few minutes.
Once the environment is up, you can hit it with your web browser. You'll get an
nginx 502 Bad Gateway message, because your server is listening on port 8080, but
the load balancer is trying to connect to port 80. We could easily fix this in the
load balancer, but for the sake of pedagogy, let's use Elastic Beanstalk's
environment configuration.

First, we need to enable websocket connections through the load balancer, which
is configured for HTTP by default.

1. In the Elastic Beanstalk console, click on **Configuration** on the left-hand
   side of the environment dashboard.
1. In the Network Tier section, click on the cogwheel next to **Load Balancing**.
1. In the Load Balancer section, simply change the Protocol to **TCP**, then
   click the **Save** button at the bottom of the page.

Elastic Beanstalk will take a couple of minutes to apply the change to the load
balancer. When it's done, you can add the bit of configuration to make your server
listen on the correct port.

Remember this bit in the server's `core.clj` file?

      (let [port (-> (System/getenv "SERVER_PORT")
                     (or "8080")
                     (Integer/parseInt))]
        (ws/run-server application {:port port, :join? false}))

This allows you to run your server on any port you want by setting an environment
variable named `SERVER_PORT`. Elastic Beanstalk makes this really easy to do:

1. In the Elastic Beanstalk console, click on **Configuration** again, then click
   on the cogwheel next to **Software Configuration**.
1. In the Environment Properties section, scroll down to the bottom and add a new
   property named `SERVER_PORT` with the value 80.
1. Click the **Save** button and wait for Elastic Beanstalk to redeploy your
   environment.

Once the environment is deployed, try it out in your websocket client by connecting
to ws://your-env-url.elasticbeanstalk.com !

## Next steps

This has been a very manual process, but I wanted to share my discoveries so
anyone else out there who is trying to figure out how to run a websocket server
whilst still utilising the awesome power of Elastic Beanstalk won't have to bang
her head against a wall for a day, like I did.

In my Github repo, I've added a [dockerise script] [9] to automate some of the
drudgery, and I'm planning to add Docker images to the [lein-beanstalk] [10]
Leiningen plugin that I use for Tomcat deployments, so I'll add a comment here
once that is done.

If you have any questions about any of this stuff, just leave a comment, and I'll
help you out if I can.

[1]: http://aws.amazon.com/elasticbeanstalk/
[2]: http://www.http-kit.org/
[3]: https://github.com/ring-clojure/ring/wiki
[4]: https://github.com/weavejester/compojure/wiki
[5]: https://www.docker.com/
[6]: https://chrome.google.com/webstore/detail/simple-websocket-client/megiodhnhnefnepmelblbmkklimncipa?hl=en-GB
[7]: https://docs.docker.com/installation/#installation
[8]: https://console.aws.amazon.com/elasticbeanstalk
[9]: https://github.com/jmglov/ws-eb-sample/blob/master/scripts/dockerise
[10]: https://github.com/weavejester/lein-beanstalk
