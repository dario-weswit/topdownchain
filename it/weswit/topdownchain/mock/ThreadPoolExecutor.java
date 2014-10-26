package it.weswit.topdownchain.mock;

public class ThreadPoolExecutor {

    public void execute(Runnable runnable) {
        new Thread(runnable).start();
    }

}