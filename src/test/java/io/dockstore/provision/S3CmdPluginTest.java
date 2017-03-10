package io.dockstore.provision;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author gluu
 * @since 07/03/17
 */
public class ICGCTest {
    S3CmdPlugin.S3CmdProvision icgcGetProvision;
    @Before
    public void before() throws Exception {
        icgcGetProvision = new S3CmdPlugin.S3CmdProvision();
        setConfiguration();
    }

    private void setConfiguration() throws Exception {
        HashMap hm = new HashMap();
        hm.put("client", "/usr/bin/s3cmd");
        hm.put("config-file-location", "/home/gluu/.s3cfg");
        icgcGetProvision.setConfiguration(hm);
    }
    @Test
    public void downloadFrom() throws Exception {
        String sourcePath = "s3cmd://asdf/asdf/thing.txt";
        Path destinationPath = Paths.get("/home/gluu/temp/file.txt");
        assertTrue(icgcGetProvision.downloadFrom(sourcePath, destinationPath));
    }

    @Test
    public void uploadTo() throws Exception {
        String destPath = "s3cmd://asdf/asdf/thing2.txt";
        Path sourceFile = Paths.get("/home/gluu/temp/file.txt");
        assertTrue(icgcGetProvision.uploadTo(destPath, sourceFile, null));
    }
}
