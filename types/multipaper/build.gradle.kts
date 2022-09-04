dependencies {
    api(project(":types:multipaper:base"))
    api(project(":types:binary:mapper:serializable"))
}


allprojects {
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    repositories {
        maven("https://repo.clojars.org/")
    }

    dependencies {
        compileOnly("net.kyori:adventure-api:4.11.0")
        compileOnly("net.md-5:bungeecord-chat:1.16-R0.4")
        compileOnly("com.github.puregero:multipaper-api:1.19.2-R0.1-SNAPSHOT")

        api(project(":types:binary"))
    }
}