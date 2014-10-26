package it.weswit.topdownchain;

public interface BaseRedirector {
    // Nota: Base per implementazioni interne efficienti
    // in quanto riutilizzabili; per poter essere riutilizzabili,
    // le implementazioni devono avere certe caratteristiche;
    // l'interfaccia non è in grado di guidare l'implementazione
    // e quindi non può essere usata per implementazioni custom

    Launcher getLauncher();
    
    public interface With1<E1 extends Exception> extends BaseRedirector {
        // ok, just a placeholder
    }

    public interface With2<E1 extends Exception, E2 extends Exception> extends BaseRedirector {
        // ok, just a placeholder
    }

    public interface With3<E1 extends Exception, E2 extends Exception, E3 extends Exception> extends BaseRedirector {
        // ok, just a placeholder
    }

    public interface With4<E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception> extends BaseRedirector {
        // ok, just a placeholder
    }

}
