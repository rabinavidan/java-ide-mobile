package javax.lang.model;

/**
 * Stub for javax.lang.model.SourceVersion, which is not present in Android's runtime.
 * ECJ's FileSystem static initializer calls SourceVersion.valueOf("RELEASE_12") to detect
 * whether the JVM supports Java 12+ features. Providing this stub allows ECJ to initialize
 * on Android without crashing.
 */
public enum SourceVersion {
    RELEASE_0, RELEASE_1, RELEASE_2, RELEASE_3, RELEASE_4,
    RELEASE_5, RELEASE_6, RELEASE_7, RELEASE_8, RELEASE_9,
    RELEASE_10, RELEASE_11, RELEASE_12, RELEASE_13, RELEASE_14,
    RELEASE_15, RELEASE_16, RELEASE_17, RELEASE_18, RELEASE_19,
    RELEASE_20, RELEASE_21;

    public static SourceVersion latest() {
        SourceVersion[] vals = values();
        return vals[vals.length - 1];
    }

    public static SourceVersion latestSupported() {
        return RELEASE_8;
    }

    public static boolean isIdentifier(CharSequence name) {
        return name != null && name.length() > 0;
    }

    public static boolean isName(CharSequence name) {
        return isIdentifier(name);
    }

    public static boolean isKeyword(CharSequence s) {
        return false;
    }
}
