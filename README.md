# s3cmd-plugin
Dockstore s3cmd file provisioning plugin.  Requires s3cmd installed along with a valid configuration file.
Run s3cmd --configure to create a configuration file at the default location.

## Usage

The s3cmd plugin is capable of downloading files by calling out to an installed copy of the [s3cmd client](http://s3tools.org/s3cmd).

```
$ cat test.json
{
  "input_file": {
        "class": "File",
        "path": "s3cmd://bucket/dir/file.txt"
    },
    "output_file": {
        "class": "File",
        "path": "s3cmd://bucket/dir/file.txt"
    }
}

$ dockstore tool launch --entry  quay.io/briandoconnor/dockstore-tool-md5sum  --json test.icgc.json
Creating directories for run of Dockstore launcher at: ./datastore//launcher-2ebce330-2a44-4a3a-9d6d-55c152a5c38e
Provisioning your input files to your local machine
Downloading: #input_file from icgc-get://FI509397 into directory: /home/gluu/dockstore/dockstore-client/./datastore/launcher-423d8d83-e6b0-418d-8a09-0e29003ac55f/inputs/0aafce03-f893-4ce2-b97f-f2c36215f162
Calling on plugin io.dockstore.provision.ICGCGetPlugin$ICGCGetProvision to provision icgc-get://FI509397
...
```


## Configuration

This plugin gets configuration information from the following structure in ~/.dockstore/config

```
[dockstore-s3cmd-plugin]
client = /usr/bin/s3cmd
config-file-location = /home/user/.s3cfg
```

Set the client location to your own s3cmd client and also make sure that the configuration file is available at the config-file-location.


