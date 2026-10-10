package com.hoangphuc.ddd.controller.support;

/**
 * Normalise phone numbers at the boundary so the DB holds ONE form of each
 * value: if a table mixes "0912 345 678" and "+84912345678", a lookup with
 * WHERE phone = ? never finds them all — and for accounts, the same person
 * could register twice.
 */
public final class Phones {

    private Phones() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String phone = raw.replaceAll("[\\s.\\-]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }
}
