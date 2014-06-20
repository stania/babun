#!/usr/bin/env groovy
import static java.lang.System.*
import static java.lang.System.getenv

VERSION = new File("${getRoot()}/babun.version").text.trim()
TEN_MINUTES = 10
TWENTY_MINUTES = 20

GROOVY = "groovy.bat"

execute()

def execute() {
    log "EXEC"
    checkArguments()
    String mode = this.args[0]
    if (mode == "clean") {
        doClean()
    } else if (mode == "cygwin") {
        doCygwin()
    } else if (mode == "package") {
        doPackage()
    } else if (mode == "release") {
        doRelease()
    }
    log "FINISHED"
}

def checkArguments() {
    if (this.args.length != 1 || !this.args[0].matches("clean|cygwin|package|release")) {
        err.println "Usage: build.groovy <clean|cygwin|package|release>"
        exit(-1)
    }
}

def initEnvironment() {
    File target = getTarget()
    if (!target.exists()) {
        target.mkdir()
    }
}

def doClean() {
    log "EXEC clean"
    File target = getTarget()
    if (target.exists()) {
        if (!target.deleteDir()) {
            throw new RuntimeException("Cannot delete targe folder")
        }
    }
}

def doPackage() {
    log "EXEC package"  
    executeBabunPackages()  
    executeBabunCygwin()
    executeBabunCore()
    executeBabunDist()
}

def doCygwin() {    
    executeBabunPackages()    
    boolean downloadOnly=true
    executeBabunCygwin(downloadOnly)
}

def doRelease() {
    log "EXEC release"
    doPackage()
    executeRelease()
}

def executeBabunPackages() {    
    String module = "babun-packages"
    log "EXEC ${module}"
	initEnvironment()
    if (shouldSkipModule(module)) return
    File workingDir = new File(getRoot(), module);
    String conf = new File(getRoot(), "${module}/conf/").absolutePath
    String out = new File(getTarget(), "${module}").absolutePath
    def command = [GROOVY, "packages.groovy", conf, out]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunCygwin(boolean downloadOnly = false) {
    String module = "babun-cygwin"
    log "EXEC ${module}"
    File workingDir = new File(getRoot(), module);
    String input = workingDir.absolutePath
    String repo = new File(getTarget(), "babun-packages").absolutePath
    String out = new File(getTarget(), "${module}").absolutePath
    String pkgs = new File(getRoot(), "babun-packages/conf/cygwin.x86.packages")
    String downOnly = downloadOnly as String
    println "Download only flag set to: ${downOnly}"
    def command = [GROOVY, "cygwin.groovy", repo, input, out, pkgs, downOnly]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunCore() {
    String module = "babun-core"
    log "EXEC ${module}"
    if (shouldSkipModule(module)) return
    File workingDir = new File(getRoot(), module);
    String root = getRoot().absolutePath
    String cygwin = new File(getTarget(), "babun-cygwin/cygwin").absolutePath
    String out = new File(getTarget(), "${module}").absolutePath    
    String branch = getenv("babun_branch") ? getenv("babun_branch") : "release"
    println "Taking babun branch [${branch}]"
    def command = [GROOVY, "core.groovy", root, cygwin, out, branch]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunDist() {
    String module = "babun-dist"
    log "EXEC ${module}"
    if (shouldSkipModule(module)) return
    File workingDir = new File(getRoot(), module);
    String input = workingDir.absolutePath
    String cygwin = new File(getTarget(), "babun-core/cygwin").absolutePath
    String out = new File(getTarget(), "${module}").absolutePath
    def command = [GROOVY, "dist.groovy", cygwin, input, out, VERSION]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeRelease() {
    log "EXEC release"
    assert getenv("bintray_user") != null
    assert getenv("bintray_secret") != null
    File artifact = new File(getTarget(), "babun-dist/babun-${VERSION}-dist.zip")
    def args = [GROOVY, "babun-dist/release/release.groovy", "babun", "babun-dist", VERSION,
            artifact.absolutePath, getenv("bintray_user"), getenv("bintray_secret")]
    executeCmd(args, getRoot(), TWENTY_MINUTES)
}

def shouldSkipModule(String module) {
    File out = new File(getTarget(), module)
    log "Checking if skip module ${module} -> folder ${out.absolutePath}"
    if (out.exists()) {
        log "SKIP ${module}"
        return true
    }
    log "DO NOT SKIP ${module}"
    return false
}

File getTarget() {
    return new File(getRoot(), "target")
}

File getRoot() {
    return new File(getClass().protectionDomain.codeSource.location.path).parentFile
}

def executeCmd(List<String> command, File workingDir, int timeout) {
    ProcessBuilder processBuilder = new ProcessBuilder(command)
    processBuilder.directory(workingDir)
    Process process = processBuilder.start()
    addShutdownHook { process.destroy() }
    process.consumeProcessOutput(out, err)
    process.waitForOrKill(timeout * 60000)
    assert process.exitValue() == 0
}

def getReleaseScript() {
    new File(getRoot(), "release.groovy")
}

def log(String msg) {
    println "[${new Date()}] ${msg}"
}
