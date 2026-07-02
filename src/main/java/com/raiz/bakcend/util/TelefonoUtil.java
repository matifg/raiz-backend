package com.raiz.bakcend.util;

public final class TelefonoUtil {

    private static final String MENSAJE_INVALIDO =
            "Teléfono inválido. Usá un móvil argentino (10 dígitos, 11 con 9, o 549 + 10 dígitos).";

    private TelefonoUtil() {
    }

    public static String normalizar(String telefono) {
        if (telefono == null) {
            return null;
        }
        String digits = telefono.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String normalizarYPersistir(String telefono) {
        validarMovilArgentino(telefono);
        return normalizar(telefono);
    }

    /** Permite null o vacío (borrar contacto); valida formato solo si hay valor. */
    public static String normalizarOpcional(String telefono) {
        String digits = normalizar(telefono);
        if (digits == null) {
            return null;
        }
        if (!esMovilArgentinoValido(digits)) {
            throw new IllegalArgumentException(MENSAJE_INVALIDO);
        }
        return digits;
    }

    public static void validarMovilArgentino(String telefono) {
        String digits = normalizar(telefono);
        if (digits == null) {
            throw new IllegalArgumentException("El teléfono es obligatorio para agentes.");
        }
        if (!esMovilArgentinoValido(digits)) {
            throw new IllegalArgumentException(MENSAJE_INVALIDO);
        }
    }

    private static boolean esMovilArgentinoValido(String digits) {
        return digits.length() == 10
                || (digits.length() == 11 && digits.startsWith("9"))
                || (digits.length() == 13 && digits.startsWith("549"));
    }
}
