plugins {
    java
    `maven-publish`
}

group = "net.mrcappy"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Only Paper API, no external shit
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    
    // Rhino for JavaScript packet filters - we bundle this
    implementation("org.mozilla:rhino:1.7.14")
    
    // Gson for webhook logging - also bundled
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    // Use Java 21
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}