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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Logger LOG = LoggerFactory.getLogger(S3CmdPlugin.class);

    public S3CmdPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        // for testing the development mode
        if (RuntimeMode.DEVELOPMENT.equals(wrapper.getRuntimeMode())) {
            System.out.println(StringUtils.upperCase("S3CmdPlugin development mode"));
        }
    }

    @Override
    public void stop() {
        System.out.println("S3CmdPlugin.stop()");
    }

    @Extension
    public static class S3CmdProvision implements ProvisionInterface {

        private static final String CLIENT_LOCATION = "client";
        private static final String CONFIG_FILE_LOCATION = "config-file-location";
        private static final String DEFAULT_CLIENT = "/usr/bin/s3cmd";
        private static final String DEFAULT_CONFIGURATION = System.getProperty("user.home") + "/.s3cfg";
        private String client;
        private String configLocation;
        private Map<String, String> config;

        void setClient(String client) {
            this.client = client;
        }

        void setConfigLocation(String configLocation) {
            this.configLocation = configLocation;
        }

        public void setConfiguration(Map<String, String> map) {
            this.config = map;
        }

        public Set<String> schemesHandled() {
            return new HashSet<>(Lists.newArrayList("s3cmd"));
        }

        /**
         * Downloads the file from the remote source path and places at the local destination
         *
         * @param sourcePath  The scheme for s3cmd (ex. s3cmd://bucket/dir/object)
         * @param destination The destination where the file is supposed to be (includes filename)
         * @return Whether download was successful or not
         */
        public boolean downloadFrom(String sourcePath, Path destination) {
            setConfigAndClient();
            // ambiguous how to reference s3cmd files, rip off these kinds of headers
            sourcePath = sourcePath.replaceFirst("s3cmd", "s3");
            String command = client + " -c " + configLocation + " get " + sourcePath + " " + destination + " --force";
            return executeConsoleCommand(command);
        }

        /**
         * This sets the s3cmd client and s3 config file based on the dockstore config file and defaults
         */
        private void setConfigAndClient() {
            if (config == null) {
                LOG.error("You are missing a dockstore config file");
            }
            if (config.containsKey(CLIENT_LOCATION)) {
                setClient(config.get(CLIENT_LOCATION));
            } else {
                setClient(DEFAULT_CLIENT);
            }
            if (config.containsKey(CONFIG_FILE_LOCATION)) {
                setConfigLocation(config.get(CONFIG_FILE_LOCATION));
            } else {
                setConfigLocation(DEFAULT_CONFIGURATION);
            }
        }

        /**
         * Uploads the local source file and places at the remote destination
         *
         * @param destPath   The remote destination (ex. s3cmd://bucket/dir/object)
         * @param sourceFile The local source file (ex. file.txt)
         * @param metadata   Metadata: currently not used
         * @return Returns true on successful upload, false otherwise
         */
        public boolean uploadTo(String destPath, Path sourceFile, Optional<String> metadata) {
            setConfigAndClient();
            destPath = destPath.replace("s3cmd://", "s3://");
            String trimmedPath = destPath.replace("s3://", "");
            List<String> splitPathList = Lists.newArrayList(trimmedPath.split("/"));
            String bucketName = splitPathList.remove(0);
            if (checkBucket("s3://" + bucketName)) {
                LOG.info("Bucket exists");
            } else {
                createBucket(bucketName);
            }
            String command = client + " -c " + configLocation + " put " + sourceFile.toString() + " " + destPath;
            return executeConsoleCommand(command);
        }

        /**
         * Check if the bucket exists
         *
         * @param bucket The actual bucket name (ex. s3://bucket)
         * @return True if bucket exists, false if bucket doesn't exist
         */
        private boolean checkBucket(String bucket) {
            String command = client + " -c " + configLocation + " info " + bucket;
            LOG.info("Bucket information: ");
            return executeConsoleCommand(command);
        }

        /**
         * Creates the bucket
         *
         * @param bucket The name of the bucket that needs to be created
         * @return True if bucket successfully created, false if it wasn't successfully created
         */
        private boolean createBucket(String bucket) {
            String command = client + " -c " + configLocation + " mb " + bucket;
            return executeConsoleCommand(command);
        }

        /**
         * Given a process, it will print out its stdout and stderr
         *
         * @param ps The process for print out
         * @return True if there's no error message, false if there is an error message
         */
        private boolean printCommandConsole(Process ps) {
            java.util.Scanner s = new java.util.Scanner(ps.getErrorStream()).useDelimiter("\\A");
            String errorString = s.hasNext() ? s.next() : "";
            if (!errorString.isEmpty()) {
                LOG.error(errorString);
            }
            return (errorString.isEmpty());
        }

        /**
         * Executes the string command given
         *
         * @param command The command to execute
         * @return True if command was successfully execute without error, false otherwise.
         */
        private boolean executeConsoleCommand(String command) {
            ProcessBuilder builder = new ProcessBuilder(command.split(" "));
            final Process p;
            try {
                p = builder.start();
                final Thread ioThread = new Thread(() -> {
                    try {
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // The first line of s3cmd plugin will start with "download", must isolate from others
                            Pattern pattern = Pattern.compile("download.*");
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.matches()) {
                                System.out.println(line);
                            } else {
                                // Output of process doesn't seem to retain the carriage returns, so manually doing that
                                System.out.print("\r" + line);
                            }

                        }
                        reader.close();
                        System.out.println();
                    } catch (IOException e) {
                        LOG.error("Could not read input stream from process. " + e.getMessage());
                    }
                });
                ioThread.start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    LOG.error("Process interrupted. " + e.getMessage());
                    return false;
                }
                return printCommandConsole(p);
            } catch (IOException e) {
                LOG.error("Could not execute command: " + command);
                return false;
            }
        }
    }
}

