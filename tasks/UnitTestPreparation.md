**Kind (Bug, Feature)

**Short Description

Preparation for the implementierung of the unit tests 

**Text

I want to implement unit tests for all classes in the following packages:

1. com.tcpviewer.proxy
2. com.tcpviewer.service
3. com.tcpviewer.service

Unfortunately at the moment multiple of the classes in question call directly JDK classes, which are difficult or impossible to mock, as fpr example:

1. java.lang.Thread
2. javafx.application.Platform

Replace such direct usages if JDK classes consistently through wrapper classes which can be easily mocked.


**Status (Open, Done, Canceled)

Done