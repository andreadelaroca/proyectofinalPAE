package ni.edu.uam.proyectofinalpae.util;

import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Async {
    private Async() {}

    public static <T> void ejecutar(Supplier<T> trabajo, Consumer<T> alExito, Consumer<Throwable> alError) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        tarea.setOnSucceeded(e -> alExito.accept(tarea.getValue()));
        tarea.setOnFailed(e -> {
            if (alError != null) alError.accept(tarea.getException());
            else tarea.getException().printStackTrace();
        });
        lanzar(tarea);
    }

    public static void ejecutarSinResultado(Runnable trabajo, Runnable alExito, Consumer<Throwable> alError) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        }, r -> {
            if (alExito != null) alExito.run();
        }, alError);
    }

    /** Mensaje apto para mostrar. Los errores inesperados se imprimen en consola. */
    public static String mensaje(Throwable error) {
        if (error == null) return "Error desconocido.";
        if (!(error instanceof NegocioException)) error.printStackTrace();
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }

    private static void lanzar(Task<?> tarea) {
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }
}