package ni.edu.uam.proyectofinalpae.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtil {
    private static final int COSTO = 12;

    private PasswordUtil() {}

    public static String hash(String plano) {
        return BCrypt.withDefaults().hashToString(COSTO, plano.toCharArray());
    }

    public static boolean verificar(String plano, String hash) {
        if (plano == null || hash == null) return false;
        return BCrypt.verifyer().verify(plano.toCharArray(), hash).verified;
    }
}
