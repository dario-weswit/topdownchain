package it.weswit.topdownchain;

public interface FirstStage {
    void invoke(Chain chain) throws RedirectedException;
}

