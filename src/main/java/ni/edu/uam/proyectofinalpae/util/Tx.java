package ni.edu.uam.proyectofinalpae.util;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

public final class Tx {
    private Tx() {}

    public static <R> R run(String errorMsg, Function<Session, R> work) {
        try (Session session = HibernateUtil.factory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = work.apply(session);
                tx.commit();
                return result;
            } catch (NegocioException e) {
                rollback(tx);
                throw e;
            } catch (Exception e) {
                rollback(tx);
                throw new RuntimeException(errorMsg, e);
            }
        }
    }

    public static void exec(String errorMsg, Consumer<Session> work) {
        run(errorMsg, s -> {
            work.accept(s);
            return null;
        });
    }

    public static <R> R read(Function<Session, R> work) {
        try (Session session = HibernateUtil.factory().openSession()) {
            return work.apply(session);
        }
    }

    private static void rollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) tx.rollback();
        } catch (Exception ignored) {
        }
    }
}