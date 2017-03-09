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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.RuntimeMode;

/**
 * @author gluu
 */
public class S3CmdPlugin extends Plugin {
    private static final Logger logger = LoggerFactory.getLogger(S3CmdPlugin.class);

    public S3CmdPlugin(PluginWrapper wrapper) {
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
    public static class S3CmdProvision implements ProvisionInterface {

        private static final String CLIENT_LOCATION = "client";
        private static final String CONFIG_FILE_LOCATION = "config-file-location";

        private Map<String, String> config;

        public void setConfiguration(Map<String, String> map) {
            this.config = map;
        }

        public Set<String> schemesHandled() {
            return new HashSet<>(Lists.newArrayList("s3cmd"));
        }

        //
        // Downloads directory will look something like:
        // .staging
        // {{Object ID folder}}
        //     - logs
        //     - {{File Name file}}
        //

        /**
         * @param sourcePath  The scheme for icgc-get (icgc-get://FI509397)
         * @param destination The destination where the file is supposed to be (includes the filename like /home/user/icgc-get/downloads/file.txt)
         * @return Whether download was successful or not
         */
        public boolean downloadFrom(String sourcePath, Path destination) {
            String client = "/home/gluu/Downloads/s3cmd-1.6.1/s3cmd";
            String configLocation = "/home/gluu/.s3cfg";
            if (config.containsKey(CLIENT_LOCATION)) {
                client = config.get(CLIENT_LOCATION);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                configLocation = config.get(CONFIG_FILE_LOCATION);
            }
            // ambiguous how to reference icgc-get files, rip off these kinds of headers
            sourcePath = sourcePath.replaceFirst("s3cmd", "s3");

            // default layout saves to original_file_name/object_id
            // file name is the directory and object id is actual file name
            String command = client + " -c " + configLocation + " get " + sourcePath + " " + destination;
            Runtime rt = Runtime.getRuntime();
            try {
                Process ps = rt.exec(command);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                printCommandConsole(ps);
            } catch (IOException e) {
                logger.info("Could not download input file");
                System.err.println(e.getMessage());
                throw new RuntimeException("Could not download input file: ", e);
            }
            return true;
        }

        public boolean uploadTo(String destPath, Path sourceFile, Optional<String> metadata) {
            destPath = destPath.replace("s3cmd://", "s3://");
            String trimmedPath = destPath.replace("s3://","");
            List<String> splitPathList = Lists.newArrayList(trimmedPath.split("/"));
            String bucketName = splitPathList.remove(0);
            checkBucket("s3://"+bucketName);
            String client = "/home/gluu/Downloads/s3cmd-1.6.1/s3cmd";
            String configLocation = "/home/gluu/.s3cfg";
            if (config.containsKey(CLIENT_LOCATION)) {
                client = config.get(CLIENT_LOCATION);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                configLocation = config.get(CONFIG_FILE_LOCATION);
            }
            // ambiguous how to reference icgc-get files, rip off these kinds of headers


            // default layout saves to original_file_name/object_id
            // file name is the directory and object id is actual file name
            String command = client + " -c " + configLocation + " put " + sourceFile.toString() + " " + destPath;
            Runtime rt = Runtime.getRuntime();
            try {
                Process ps = rt.exec(command);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                printCommandConsole(ps);
                return true;
            } catch (IOException e) {
                logger.info("Could not download input file");
                System.err.println(e.getMessage());
                throw new RuntimeException("Could not download input file: ", e);
            }
        }

        private boolean checkBucket(String bucket) {
            String client = "/home/gluu/Downloads/s3cmd-1.6.1/s3cmd";
            String configLocation = "/home/gluu/.s3cfg";
            if (config.containsKey(CLIENT_LOCATION)) {
                client = config.get(CLIENT_LOCATION);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                configLocation = config.get(CONFIG_FILE_LOCATION);
            }
            String command = client + " -c " + configLocation + " info " + bucket;
            Runtime rt = Runtime.getRuntime();
            try {
                Process ps = rt.exec(command);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                java.util.Scanner s = new java.util.Scanner(ps.getErrorStream()).useDelimiter("\\A");
                String errorString = s.hasNext() ? s.next() : "";
                System.out.println("Error String: " + errorString);

                java.util.Scanner s2 = new java.util.Scanner(ps.getInputStream()).useDelimiter("\\A");
                String inputString = s2.hasNext() ? s2.next() : "";
                System.out.println("Input String: " + inputString);
                if (errorString.isEmpty()) {
                    System.out.println("Bucket is present");
                    return true;
                }
                else {
                    System.out.println("Bucket is not present");
                    createBucket(bucket);
                    return false;
                }
            } catch (IOException e) {
                logger.info("Could not download input file");
                System.err.println(e.getMessage());
                throw new RuntimeException("Could not download input file: ", e);
            }
        }

        private boolean createBucket(String bucket) {
            String client = "/home/gluu/Downloads/s3cmd-1.6.1/s3cmd";
            String configLocation = "/home/gluu/.s3cfg";
            if (config.containsKey(CLIENT_LOCATION)) {
                client = config.get(CLIENT_LOCATION);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                configLocation = config.get(CONFIG_FILE_LOCATION);
            }
            String command = client + " -c " + configLocation + " mb " + bucket;
            Runtime rt = Runtime.getRuntime();
            try {
                Process ps = rt.exec(command);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                printCommandConsole(ps);
                System.out.println("Made bucket: " + bucket);
                return true;
            } catch (IOException e) {
                logger.info("Could not download input file");
                System.err.println(e.getMessage());
                throw new RuntimeException("Could not download input file: ", e);
            }
        }

        private void printCommandConsole(Process ps) {
            java.util.Scanner s = new java.util.Scanner(ps.getErrorStream()).useDelimiter("\\A");
            String errorString = s.hasNext() ? s.next() : "";
            System.out.println("Error String: " + errorString);

            java.util.Scanner s2 = new java.util.Scanner(ps.getInputStream()).useDelimiter("\\A");
            String inputString = s2.hasNext() ? s2.next() : "";
            System.out.println("Input String: " + inputString);
        }
    }
}

