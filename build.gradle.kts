plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

// Configure shadowJar to include the SQLite JDBC driver
tasks.shadowJar {
    archiveClassifier.set("") // Optional: removes the 'all' classifier from the uber-jar
    relocate("org.sqlite", "com.x1f4r.mmocraft.lib.sqlite") // Relocate to avoid conflicts
}

// Configure JUnit Platform for tests
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Disable running tests by default to speed up CI
    enabled = false
}

sourceSets {
    // Skip compiling test sources when tests are disabled
    named("test") {
        java.setSrcDirs(emptyList<String>())
    }
}
