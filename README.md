App Engine RSS Feeds
Copyright (C) 2010-2012 FeiYang Inc.

## Sample rss feeds for use with App Engine Java.

Requires [Apache Maven](http://maven.apache.org) 3.0 or greater, and JDK 6+ in order to run.

To build, run

    mvn package

Building will run the tests, but to explicitly run tests you can use the test target

    mvn test

To start the app, use the [App Engine Maven Plugin](http://code.google.com/p/appengine-maven-plugin/) that is already included in this demo.  Just run the command.

    mvn appengine:devserver

For further information, consult the [Java App Engine](https://developers.google.com/appengine/docs/java/overview) documentation.

Then open browser with URL http://localhost:8080/feed/categories.json

To see all the available goals for the App Engine plugin, run

    mvn help:describe -Dplugin=appengine
    
Quick Start

	http://localhost:8080/registry.json?name=test
	http://localhost:8888/category/create.json?uid=${uid}&name=testcategory
	http://localhost:8888/category/subscribe.json?uid=${uid}&cid=${categoryId}&site=http://www.huxiu.com/rss/0.xml
	http://localhost:8888/view/home.json?uid=${uid}