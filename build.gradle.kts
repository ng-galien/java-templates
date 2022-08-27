plugins {
    java
}

group "org.example"
version "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-core:1.2.11")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("org.apache.camel:camel-test-junit5:3.18.1")
    testImplementation("org.apache.camel:camel-endpointdsl:3.18.1")
}

tasks.test {
    useJUnitPlatform()
}