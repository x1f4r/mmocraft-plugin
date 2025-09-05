plugins {
    id("java")
    id("com.gradleup.shadow") version "9.1.0"
}

group = "com.x1f4r.mmocraft"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.21.5-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0") // Added SQLite JDBC driver

    // Testing dependencies
    testImplementation("org.purpurmc.purpur:purpur-api:1.21.5-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0") // For @ExtendWith(MockitoExtension.class)
}

java {
    // Use the system JDK if Java 17 is unavailable
    val javaVersion = if (JavaVersion.current() >= JavaVersion.VERSION_21) 21 else 17
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

// Configure shadowJar to include the SQLite JDBC driver
tasks.shadowJar {
    archiveClassifier.set("") // Optional: removes the 'all' classifier from the uber-jar
    mergeServiceFiles() // Required for shaded JDBC drivers to work correctly
    // Do not relocate org.sqlite, as it breaks native library loading on some platforms (e.g., macOS)
    // relocate("org.sqlite", "com.x1f4r.mmocraft.lib.sqlite")
}

// Configure JUnit Platform for tests
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Re-enable tests
    enabled = true
}

sourceSets {
    // Re-enable the test source directory
    named("test") {
        java.setSrcDirs(listOf("src/test/java"))
    }
}
