plugins {
    application
}

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
