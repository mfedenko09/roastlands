plugins {
    application
}

dependencies {
    implementation(project(":knowledge-core"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "io.github.mfedenko09.roastlands.content.TopicMapCli"
}

val packsRoot = rootProject.layout.projectDirectory.dir("content/packs")
val topicMapDoc = rootProject.layout.projectDirectory.file("docs/topic-map.md")

tasks.register<JavaExec>("generateTopicMap") {
    group = "documentation"
    description = "Writes docs/topic-map.md from content/packs"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("generate", packsRoot.asFile.path, topicMapDoc.asFile.path)
    inputs.dir(packsRoot)
    outputs.file(topicMapDoc)
}

val verifyTopicMap = tasks.register<JavaExec>("verifyTopicMap") {
    group = "verification"
    description = "Fails when the topic map in content/packs is broken"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("verify", packsRoot.asFile.path)
    inputs.dir(packsRoot)
}

tasks.test {
    inputs.dir(packsRoot)
    inputs.file(topicMapDoc)
}

tasks.check {
    dependsOn(verifyTopicMap)
}
