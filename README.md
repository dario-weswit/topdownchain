topdownchain
============

Library code related to article:
[Exploiting Static Exception Checking in Asynchronous Java Code](http://blog.lightstreamer.com/2014/07/exception-handling-in-asynchronous-java.html).

Notes:
- Code is commented only where strictly needed; comments are in Italian.
- Needs the [Javassist](www.javassist.org) library to compile and execute.

Package organization:

- it.weswit.topdownchain<br>
Core library.
- it.weswit.topdownchain.redirection<br>
Optional extensions to adapt common APIs to the library interface.
- it.weswit.topdownchain.mock<br>
Simple alternative versions of some common APIs that help simplify the tests.
- it.weswit.topdownchain.test<br>
Various test programs; in particular:<br>
Usage.java shows the syntax examples in the article (not for running);<br>
UnitTest.java tests all combinations of try-catch-finally cases;<br>
Example.java tests a chain made up of several different stages;<br>
Sample.java tests the concrete example reported in chapter 6.<br>
