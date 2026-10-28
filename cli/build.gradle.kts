plugins {
    application
}

dependencies {
    implementation(project(":knowledge-core"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    applicationName = "roastlands-cli"
    mainClass = "io.github.mfedenko09.roastlands.cli.RoastlandsCli"
    applicationDefaultJvmArgs = listOf("-Dstdout.encoding=UTF-8", "-Dstdin.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    standardInput = System.`in`
}
