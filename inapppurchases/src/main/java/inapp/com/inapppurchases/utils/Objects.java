package inapp.com.inapppurchases.utils;

public class Objects {
    public static <T> T requireNonNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }
}
