# s3-plugin
Dockstore S3 file provisioning plugin

## Usage

The s3 plugin is capable of download, upload, and can set metadata on uploaded objects. 

```
$ cat test.s3.json 
{
  "input_file": {
        "class": "File",
        "path": "s3://oicr.temp/bamstats_report.zip"
    },
    "output_file": {
        "class": "File",
        "metadata": "eyJvbmUiOiJ3b24iLCJ0d28iOiJ0d28ifQ==",
        "path": "s3://oicr.temp/bamstats_report.zip"
    }
}

$ dockstore tool launch --entry  quay.io/briandoconnor/dockstore-tool-md5sum  --json test.s3.json
Creating directories for run of Dockstore launcher at: ./datastore//launcher-a246f1b6-21fd-468e-8780-b064d311dda5
Provisioning your input files to your local machine
Downloading: #input_file from s3://oicr.temp/bamstats_report.zip into directory: /media/large_volume/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-21fd-468e-8780-b064d311dda5/inputs/73b70f11
-1711-40b7-bfea-9ee4543a8226
Found file s3://oicr.temp/bamstats_report.zip in cache, hard-linking
Calling on plugin io.dockstore.provision.S3Plugin$S3Provision to provision s3://oicr.temp/bamstats_report.zip
Calling out to cwltool to run your tool
Executing: cwltool --enable-dev --non-strict --outdir /media/large_volume/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-21fd-468e-8780-b064d311dda5/outputs/ --tmpdir-prefix /media/large_volu
me/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-21fd-468e-8780-b064d311dda5/tmp/ --tmp-outdir-prefix /media/large_volume/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-
21fd-468e-8780-b064d311dda5/working/ /tmp/1488407859906-0/temp3047430238970788171.cwl /media/large_volume/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-21fd-468e-8780-b064d311dda5/workflow_p
arams.json
/usr/local/bin/cwltool 1.0.20170217172322
...
Provisioning your output files to their final destinations
Uploading: #output_file from /media/large_volume/dockstore_tools/dockstore-tool-md5sum/./datastore/launcher-a246f1b6-21fd-468e-8780-b064d311dda5/outputs/md5sum.txt to : s3://oicr.temp/bamstats_report.zip
Calling on plugin io.dockstore.provision.S3Plugin$S3Provision to provision to s3://oicr.temp/bamstats_report.zip
Loading one->won
Loading two->two
```

Note that metadata is Base64 encoded in the JSON and creates metadata tags on the uploaded file. 

## Configuration 

This plugin gets configuration information from the following structure in ~/.dockstore/config

```
[dockstore-file-s3-plugin]
endpoint = <endpoint> 
```

Set the endpoint to a different value in order to talk to a S3 endpoint that is not the official endpoint hosted by AWS. (ex: https://object.cancercollaboratory.org:9080 )
Note that the standard [Configuration and Credential Files](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-config-files) should be setup in your home directory in order to set an access key and secret access key. 

