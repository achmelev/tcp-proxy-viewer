**Kind (Bug, Feature)

Feature

**Short Description

Writing unit tests

**Text

As of now the codebase has an entirely isufficient unit tests coverage. Implement unit tests for all classes except

1. The configuration classes from the package com.tcpviewer.config
2. The wrapper classes from the **.wrapper packages
3. The ui classes from the package com.tcpviewer.ui

Follow the following guidelines:

1. Use Mockito as unit testing framework
2. Write one unit test class for every application class
3. Place unit test classes in the same package as  the the tested class
4. Unit test class should only test the function of one class. So use mocks for the referenced classes.
5. Only implement mocks for the classes from the codebase. If skip the problematic class and point to the problem in your reply.

**Status (Open, Done, Canceled)

Done
