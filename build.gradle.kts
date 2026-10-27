plugins {
    java
}

group = "info.plugin.minecraft"
version = "1.0.0"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}