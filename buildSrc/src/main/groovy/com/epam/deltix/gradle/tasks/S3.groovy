package com.epam.deltix.gradle.tasks

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.event.ProgressListener
import com.amazonaws.event.ProgressEvent
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.text.DecimalFormat
import java.nio.file.Path

abstract class S3Task extends DefaultTask {
    @Input
    String bucket

    @Input
    String region

    String getBucket() { bucket }

    def getS3Client() {
        //def profileCreds

//        if (profile) {
//            logger.info("Using AWS credentials profile: ${profile}")
//            profileCreds = new ProfileCredentialsProvider(profile)
//        }
//        else {
//            profileCreds = new ProfileCredentialsProvider()
//        }

        def credentials

        def accessKey = project.findProperty('AWS_ACCESS_KEY_ID') ?: System.getenv('AWS_ACCESS_KEY_ID') ?: null
        def secretKey = project.findProperty('AWS_SECRET_ACCESS_KEY') ?: System.getenv('AWS_SECRET_ACCESS_KEY') ?: null

        if (accessKey && secretKey) {
            credentials = new AWSCredentialsProviderChain(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
        } else {
            credentials = new AWSCredentialsProviderChain(
                    new EnvironmentVariableCredentialsProvider(),
                    new SystemPropertiesCredentialsProvider(),
                    new EC2ContainerCredentialsProviderWrapper()
            )
        }

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withCredentials(credentials);
        if (region) {
            builder = builder.withRegion(region);
        }

        AmazonS3 s3Client = builder.build();
        s3Client
    }
}

class S3Upload extends S3Task {
    @Input
    String key

    @Input
    String file

    @Input
    boolean overwrite = false

    @TaskAction
    def task() {
        if (s3Client.doesObjectExist(bucket, key)) {
            if (overwrite) {
                logger.warn("S3 Upload ${file} → s3://${bucket}/${key} with overwrite")
                s3Client.putObject(bucket, key, new File(file))
            }
            else {
                logger.warn("s3://${bucket}/${key} exists, not overwriting")
            }
        }
        else {
            logger.warn("S3 Upload ${file} → s3://${bucket}/${key}")
            s3Client.putObject(bucket, key, new File(file))
        }

        s3Client.setObjectAcl(bucket, key, CannedAccessControlList.PublicRead);
    }
}


class S3Download extends S3Task {
    String key
    String file
    String keyPrefix
    String destDir

    @TaskAction
    def task() {
        TransferManager tm = new TransferManager()
        Transfer transfer
        Path temp

        // directory download
        if (keyPrefix != null) {
            logger.info("S3 Download recursive s3://${bucket}/${keyPrefix} → ${project.file(destDir)}/")
            transfer = tm.downloadDirectory(bucket, keyPrefix, project.file(destDir))
        }

        // single file download
        else {
            logger.info("S3 Download s3://${bucket}/${key} → ${file}")
            File f = new File(file)
            f.parentFile.mkdirs()
            transfer = tm.download(bucket, key, f)
        }

        def listener = new S3Listener()
        listener.transfer = transfer
        transfer.addProgressListener(listener)
        transfer.waitForCompletion()
    }

    class S3Listener implements ProgressListener {
        Transfer transfer

        DecimalFormat df = new DecimalFormat("#0.0")
        public void progressChanged(ProgressEvent e) {
            logger.info("${df.format(transfer.progress.percentTransferred)}%")
        }
    }
}
