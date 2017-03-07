/*
 *    Copyright 2016 OICR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.dockstore.provision;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.RuntimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.IOUtils;

import static jdk.nashorn.internal.runtime.regexp.joni.Syntax.Java;

/**
 * @author gluu
 */
public class ICGCGetPlugin extends Plugin {
    private static final Logger logger = LoggerFactory.getLogger(ICGCGetPlugin.class);

    public ICGCGetPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        // for testing the development mode
        if (RuntimeMode.DEVELOPMENT.equals(wrapper.getRuntimeMode())) {
            System.out.println(StringUtils.upperCase("ICGCStorageClientPlugin development mode"));
        }
    }

    @Override
    public void stop() {
        System.out.println("ICGCGetPlugin.stop()");
    }

    @Extension
    public static class ICGCGetProvision implements ProvisionInterface {

        private static final String CLIENT_LOCATION = "client";
        private static final String CONFIG_FILE_LOCATION = "config-file-location";

        private Map<String, String> config;

        public void setConfiguration(Map<String, String> map) {
            this.config = map;
        }

        public Set<String> schemesHandled() {
            return new HashSet<>(Lists.newArrayList("icgc"));
        }

        public boolean downloadFrom(String sourcePath, Path destination) {
            String client = "/icgc-get/icgc-get";
            String configLocation = "/.icgc-get/config.yaml";
            String fileID = null;

            if (config.containsKey(CLIENT_LOCATION)) {
                client = config.get(CLIENT_LOCATION);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                configLocation = config.get(CONFIG_FILE_LOCATION);
            }

            // ambiguous how to reference icgc-get files, rip off these kinds of headers
            String prefix = "icgc-get://";
            if (sourcePath.startsWith(prefix)){
                fileID = sourcePath.substring(prefix.length());
            }

            // default layout saves to original_file_name/object_id
            // file name is the directory and object id is actual file name
            String downloadDir = destination.getParent().toFile().getAbsolutePath();
            String bob = client + " --config " + configLocation + " download " + fileID + " --output " + destination;
            Runtime rt = Runtime.getRuntime();
            try {
                Process ps = rt.exec(bob);
                java.util.Scanner s = new java.util.Scanner(ps.getErrorStream()).useDelimiter("\\A");
                String errorString = s.hasNext() ? s.next() : "";
                System.out.println("Error String: " + errorString);

                java.util.Scanner s2 = new java.util.Scanner(ps.getInputStream()).useDelimiter("\\A");
                String inputString = s2.hasNext() ? s2.next() : "";
                System.out.println("Input String: " + inputString);
                return true;

            } catch (IOException e) {
                logger.info("Could not download input file");
                System.err.println(e.getMessage());
                throw new RuntimeException("Could not download input file: ", e);
            }
        }

        public boolean uploadTo(String destPath, Path sourceFile, Optional<String> metadata) {
            throw new UnsupportedOperationException("ICGC Get does not support upload");
        }

    }

}

