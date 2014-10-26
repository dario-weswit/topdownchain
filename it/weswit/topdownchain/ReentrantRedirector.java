package it.weswit.topdownchain;


public abstract class ReentrantRedirector implements BaseRedirector {
    // Nota: Base per implementazioni interne efficienti
    // in quanto riutilizzabili; per poter essere riutilizzabili,
    // le implementazioni devono avere certe caratteristiche;
    // l'interfaccia non è in grado di guidare l'implementazione
    // e quindi non può essere usata per implementazioni custom

    private final Launcher unique;
    
    protected ReentrantRedirector() {
        this.unique = getUniqueLauncher();
    }
    
    protected abstract Launcher getUniqueLauncher();
    
    @Override
    public final Launcher getLauncher() {
        return unique;
    }
}