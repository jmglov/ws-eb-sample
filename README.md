# ws-eb-sample

Sample websocket server on AWS Elastic Beanstalk

## Usage

See the `BLOG.md` file for a tutorial.

### Running locally

```
lein run
```

Connect to ws://localhost:8080/ws with a websocket client (try the excellent
[Simple WebSocket Client] [1] Chrome extension).

### Docker

```
./scripts/dockerise local
./scripts/dockerise run
```

Connect to ws://localhost:8080/ws as above.

### Docker on Elastic Beanstalk

After creating a Docker environment and setting the SERVER_PORT environment


```
./scripts/dockerise beanstalk
```

Create an application version in Elastic Beanstalk, uploading the
`target/ws-eb-sample.zip` file, then deploy the version.

Hit your Elastic Beanstalk environment on port 80

## License

Copyright © 2014 Josh Glover <jmglov@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://chrome.google.com/webstore/detail/simple-websocket-client/megiodhnhnefnepmelblbmkklimncipa?hl=en-GB
