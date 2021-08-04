package com.epam.deltix.gradle.tasks.installer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class BuildTimebaseInstallerTask extends DefaultTask {

    static final PROJECT_DIR = 'izpack/timebase'
    static final RESOURCES_DIR = PROJECT_DIR + '/resources'
    static final INSTALL_XML_FILE = 'install.xml'

    @Input
    String classpath

    @Input
    String jarpath

    @Input
    String platform

    @Input
    String[] customPacks

    @Input
    File generatedDirectory

    @Input
    String instDirectory

    @Input
    String instVersion

    @Input
    String tbVersion

    @Input
    @Optional
    String jreVersion = null

    @Input
    String localRepositoryPath = ""

    BuildTimebaseInstallerTask() {
        project.apply plugin: 'de.undercouch.download'
    }

    @TaskAction
    void buildInstaller() {
        if (customPacks.length == 0) {
            throw new RuntimeException("QuantServer and QuantOffice are skipped. Nothing to build.")
        }

        def projectDir = new File(project.projectDir, PROJECT_DIR)
        def resourcesDir = new File(project.projectDir, RESOURCES_DIR)
        def installXmlFile = new File(projectDir.absolutePath, INSTALL_XML_FILE)

        String installerDir = "${instDirectory}/installer"
        String outputInstaller = "${installerDir}/timebase-${platform}-installer-${instVersion}.jar"

        if (!new File(installerDir).mkdirs()) {
            println "Directory " + installerDir + " can't be created"
        }
        
        println "Building installer"
        println "Project dir: " + projectDir
        println "Resources dir: " + resourcesDir
        println "Generated dir: " + generatedDirectory
        println "Install xml file: " + installXmlFile

//        new File(project.getRootDir(), PLUGINS_DIR).mkdirs() // make sure plugins directory created (because it can be switched off)
        
//        downloadExternalResources(resourcesDir, generatedDirectory, distDir)

        ant.taskdef(
                name: "izpack_${platform}",
                classpath: classpath,
                classname: 'com.izforge.izpack.ant.IzPackTask'
        )

        ant.setProperty('panels.jar', jarpath)

        ant.setProperty('compiledPlatform', platform)
        ant.setProperty('version', tbVersion)
        ant.setProperty('appname', 'Timebase')
        ant.setProperty('tb.appname', 'Timebase')
        ant.setProperty('tb.version', tbVersion)
        ant.setProperty('instVersion', instVersion)

        ant.setProperty('izpack.path', project.getRootProject().relativePath(projectDir))
        ant.setProperty('izpack.resources.path', project.getRootProject().relativePath(resourcesDir))
        ant.setProperty('izpack.generated.path', project.getRootProject().relativePath(generatedDirectory))

        ant.setProperty('packs', getPacksDefinition(resourcesDir))

        ant."izpack_${platform}"(
                basedir: project.getRootDir(),
                output: outputInstaller)
        {
            ant.config(installXmlFile.text)
        }

        println "Installer created: " + outputInstaller
    }

//    private void downloadExternalResources(File resourcesDir, File generatedDirectory, String distDir) {
//        String[] resourceFiles = getResourcesFileList()
//        ArrayList<String[]> resources = new ArrayList<>()
//        for (int i = 0; i < resourceFiles.length; ++i) {
//            resources.addAll(
//                buildResourceList(
//                    new File(resourcesDir, "${platform}/${resourceFiles[i]}"),
//                    ['qoVersion': qoVersion,
//                     'jreVersion': jreVersion],
//                    generatedDirectory.absolutePath
//                )
//            )
//        }
//
//        generatedDirectory.mkdirs()
//        for (int i = 0; i < resources.size(); ++i) {
//            downloadResource(distDir, resources.get(i)[0], resources.get(i)[1]);
//        }
//    }

//    private void downloadResource(String local, String sourceFile, String destFolder) {
//        try {
//            println "download file:///${local}/${sourceFile}"
//            project.download {
//                src "file:///${local}/${sourceFile}"
//                dest destFolder
//            }
//        } catch (Exception e) {
//            try {
//                println "download file:///${localRepositoryPath}/${sourceFile}"
//                project.download {
//                    src "file://$localRepositoryPath/$sourceFile"
//                    dest destFolder
//                }
//            } catch (Exception ex) {
//                println "download http://gw.deltixlab.com/install/dist/${sourceFile}"
//                project.download {
//                    src "http://gw.deltixlab.com/install/dist/${sourceFile}"
//                    dest destFolder
//                }
//            }
//        } finally {
//            System.out.println("Failed to download file: " + sourceFile);
//        }
//    }
//
//    private static ArrayList<String[]> buildResourceList(File resourceFile, Map bindings, String destDir) {
//        ArrayList<String[]> resources = new ArrayList<String[]>()
//        if (!resourceFile.exists())
//            return resources
//
//        def templateEngine = new SimpleTemplateEngine()
//        resourceFile.eachLine { line ->
//            if (line.isEmpty())
//                return
//
//            def splittedLine = templateEngine.createTemplate(line).make(bindings).toString().split(',')
//            if (splittedLine.length >= 2)
//                resources.add([splittedLine[0], destDir + '/' + splittedLine[1]].toArray(new String[2]))
//        }
//
//        return resources
//    }

    private String getPacksDefinition(File resourcesDir) {
        String[] packs = [] //getPacksFileList()

        String packsDefinition = '';
        for (int i = 0; i < packs.length; ++i) {
            String packFile = resourcesDir.absolutePath + "/" + platform + "/" + packs[i]
            println "Included pack: " + packFile
            packsDefinition = packsDefinition + packInclude(packFile)
        }

        for (int i = 0; i < customPacks.length; ++i) {
            String packFile = customPacks[i]
            println "Included pack: " + packFile
            packsDefinition = packsDefinition + packInclude(packFile)
        }

        return packsDefinition
    }

    private String packInclude(String file) {
        return "<xi:include href=\"${file}\" xmlns:xi=\"http://www.w3.org/2001/XInclude\"/>\n"
    }

//    private String[] getResourcesFileList() {
//        List<String> resources = [QUANT_SERVER_EXTERNAL_FILE, QUANT_OFFICE_EXTERNAL_FILE]
//        if (skipQO) {
//            resources.remove(QUANT_OFFICE_EXTERNAL_FILE)
//        }
//        if (skipQS) {
//            resources.remove(QUANT_SERVER_EXTERNAL_FILE)
//        }
//
//        return resources
//    }

}
