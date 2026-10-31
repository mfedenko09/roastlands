plugins {
    application
    alias(libs.plugins.beryx.runtime)
}

version = "0.1.0"

dependencies {
    implementation(project(":knowledge-core"))
    implementation(libs.jme.core)
    implementation(libs.jme.desktop)
    implementation(libs.jme.lwjgl3)
    implementation(libs.jme.plugins)
    implementation(libs.minie)
    implementation(libs.lemur)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    applicationName = "roastlands"
    mainClass = "io.github.mfedenko09.roastlands.game.Roastlands"
    applicationDefaultJvmArgs = listOf("-Xmx1g")
}

val contentDir = rootProject.layout.projectDirectory.dir("content")

tasks.processResources {
    from(contentDir) {
        include("packs/**")
        into("content")
    }
}

val verifyPackedContent = tasks.register<JavaExec>("verifyPackedContent") {
    group = "verification"
    description = "Fails when the game jar does not hold the same questions as content/"
    classpath = files(tasks.jar) + configurations.runtimeClasspath.get()
    mainClass = "io.github.mfedenko09.roastlands.game.PackedContentCheck"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    args(contentDir.asFile.path)
    inputs.dir(contentDir)
}

tasks.check {
    dependsOn(verifyPackedContent)
}

runtime {
    options = listOf("--strip-debug", "--no-header-files", "--no-man-pages")
    modules = listOf("java.base", "java.desktop", "java.logging", "jdk.unsupported", "jdk.zipfs")
    jpackage {
        imageName = "Roastlands"
        imageOptions = listOf("--icon", file("src/main/jpackage/roastlands.ico").path)
    }
}

// The Java launcher reads its own path in the system code page, so a Cyrillic folder on a Windows with another
// code page stops the start. UTF-8 as the active code page of the exe removes this limit.
val useUtf8InLauncher = tasks.register<Exec>("useUtf8InLauncher") {
    val packaging = layout.projectDirectory.dir("src/main/jpackage")
    val exe = layout.buildDirectory.file("jpackage/Roastlands/Roastlands.exe")
    onlyIf { System.getProperty("os.name").startsWith("Windows") }
    commandLine(
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", packaging.file("replace-manifest.ps1").asFile.path,
        "-Exe", exe.get().asFile.path,
        "-Manifest", packaging.file("Roastlands.exe.manifest").asFile.path,
    )
}

tasks.jpackageImage {
    dependsOn(verifyPackedContent)
    finalizedBy(useUtf8InLauncher)
}

val renderingClasses = listOf(
    "Roastlands", "RoastlandsApp", "IslandState", "PlayerState", "PhysicsAwareState", "InputBindings",
    "StartupErrorDialog", "Widgets", "QuestionPanelState", "TerminalState", "ProgressMapState", "HudState",
).flatMap { name ->
    val path = "io/github/mfedenko09/roastlands/game/$name"
    listOf("$path.class", "$path$*.class")
}

val testableClasses = sourceSets.main.get().output.classesDirs.asFileTree.matching { exclude(renderingClasses) }

tasks.jacocoTestReport {
    classDirectories.setFrom(testableClasses)
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(testableClasses)
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
