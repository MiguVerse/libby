plugins {
    id("net.byteflux.java-conventions")
    id("net.kyori.blossom").version("2.2.0")
}

dependencies {
    api("com.grack:nanojson:1.7")

    compileOnly("org.apache.maven.resolver:maven-resolver-supplier:1.9.25")
    compileOnly("org.apache.maven:maven-resolver-provider:3.9.11")
}

sourceSets {
    main {
        blossom {
            resources {
                property("version", project.version.toString())
                property("httpUserAgent", "libby/" + project.version.toString())
            }
        }
    }
}

description = "libby-core"
