/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
import com.kniazkov.widgets.base.ReleaseInfo;

/**
 * Reads the identity of the distribution selected by the caller's classpath.
 */
public final class BuildId {
    private BuildId() {
    }

    public static void main(final String[] args) {
        System.out.println(ReleaseInfo.buildId());
    }
}
