package it.weswit.topdownchain.mock;

public class Logger {
    
    public void error(String msg, Throwable t) {
        System.out.println(msg);
        t.printStackTrace();
    }

}
