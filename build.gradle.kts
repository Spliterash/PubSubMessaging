plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "6.5.1" apply false
}
val javaProjects by extra { allprojects - project(":publish") }

allprojects {
    version = "1.1.3"
}


configure(javaProjects) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.13")

        testImplementation(platform("org.junit:junit-bom:5.9.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.mockito:mockito-core:3.+")
    }
    tasks.test {
        useJUnitPlatform()
    }

    if (!file("src").isDirectory) {
        tasks.jar {
            enabled = false;
        }
        return@configure;
    }
    apply(plugin = "io.freefair.lombok")

    java {
        withSourcesJar()
    }
}

configure(javaProjects - project(":core")) {
    dependencies {
        api(project(":core"))
    }
}