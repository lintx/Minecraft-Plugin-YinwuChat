package org.lintx.plugins.yinwuchat;

import org.junit.Test;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** Web assets must survive Maven resource processing verbatim. Author: Soidraw. */
public class WebResourcePackagingTest {
    @Test
    public void copiesWebAssetsWithoutInterpolatingJavaScript() throws Exception {
        for (String name : new String[]{"index.html", "logo.png", "forge.min.js"}) {
            try (InputStream resource = getClass().getResourceAsStream("/web/" + name)) {
                assertNotNull(name, resource);
                assertArrayEquals(name, Files.readAllBytes(Path.of("src/main/resources/web", name)), resource.readAllBytes());
            }
        }
    }
}
