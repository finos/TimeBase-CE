package com.epam.deltix.gradle.tasks.proget

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 *
 */
class DownloadPackageTask extends DefaultTask  {

    enum FeedType {
        MAVEN,
        NUGET,
        NUGET_NEXUS,
        ARCHIVE,
        PYTHON
    }

    enum ArchiveFormat {
        ZIP,
        TAR
    }

    @Input
    String repository

    @Input
    ArchiveFormat archiveFormat = ArchiveFormat.ZIP

    @Input
    FeedType feedType

    @Input
    String[] packages

    @Input
    String group = ''

    @Input
    String version

    @Input
    String user

    @Input
    String pass

    @Input
    String[] unpackDirs = []

    @OutputDirectory
    File outputDir

    DownloadPackageTask() {
        project.apply plugin: 'de.undercouch.download'
    }

    @TaskAction
    void download() {
        for (packageId in packages) {
            download(packageId);
        }
    }

    void download(String packageId) {
        def tempDir = new File(project.file('build/tmp'), File.createTempDir().name)
        tempDir.deleteOnExit()

        def archiveExtension = archiveFormat == ArchiveFormat.ZIP ? 'zip' : 'tar.gz'
        def tempFile = new File(tempDir, "package_${packageId}_${version}.${archiveExtension}")
        def subpath = getSubPath()
        def srcUrl;
        if (feedType == FeedType.ARCHIVE) {
            srcUrl = "${repository}${subpath}/${packageId}/${packageId}-${version}.${archiveExtension}"
        } else if (feedType == FeedType.PYTHON) {
            srcUrl = "${repository}/packages/${packageId}/${version}/${packageId}-${version}-py2.py3-none-any.whl"
        } else {
            srcUrl = "${repository}${subpath}/${packageId}/${version}"
        }

        logger.info("Downloading package from ${srcUrl}")
        try {
            project.download {
                src srcUrl
                dest tempFile.absolutePath
                username user
                password pass
            }
        } catch (Exception ignored) {
            System.out.println("Failed to download file: " + srcUrl);
        }

        String[] dirs = unpackDirs
        extractDirectory(tempFile, dirs)

        println("Package ${packageId} downloaded and unpacked to: ${outputDir.absolutePath}")
    }

    String getSubPath() {
        switch (feedType) {
            case FeedType.ARCHIVE:
                return ''
            case FeedType.NUGET_NEXUS:
                return ''
            default:
                return '/package'
        }
    }



    void extractDirectory(File zipFile, String[] unpackDirs) {
        if (unpackDirs == null || unpackDirs.length == 0) {
            project.copy {
                from archiveFormat == ArchiveFormat.ZIP ? project.zipTree(zipFile.absolutePath) : project.tarTree(zipFile.absolutePath)
                into outputDir.absolutePath
            }
        } else {
            for (int i = 0; i < unpackDirs.length; ++i) {
                project.copy {
                    from archiveFormat == ArchiveFormat.ZIP ? project.zipTree(zipFile.absolutePath) : project.tarTree(zipFile.absolutePath)
                    into outputDir.absolutePath
                    include "${unpackDirs[i]}/**"
                    eachFile { f ->
                        f.path = f.path.replaceFirst("${unpackDirs[i]}", '')
                    }
                    includeEmptyDirs false
                }
            }
        }

    }

    private tmp(String x) {
        project.zipTree(x)
    }

    private archiveToTree(x) {
        /*
        switch (archiveFormat) {
            case ArchiveFormat.ZIP:
                project.zipTree(path)
                break
            case ArchiveFormat.TAR:
                project.tarTree(path)
            default:
                throw new IllegalStateException("Wrong format: " + archiveFormat);
        }
        */
        project.zipTree(x)
    }
}
