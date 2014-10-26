package it.weswit.topdownchain;

public interface ChainOutcomeListener {
    void onClose(Throwable currThrown);
}