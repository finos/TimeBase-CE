package com.epam.deltix.gradle.tasks.proget

/**
 * Created by Alex Karpovich on 1/2/2019.
 */
class NuGetDownload extends DownloadPackageTask {

    NuGetDownload() {
        repository = 'https://www.nuget.org/api/v2'
        feedType = 'NUGET'
        pass = ''
        user = ''
    }
}
