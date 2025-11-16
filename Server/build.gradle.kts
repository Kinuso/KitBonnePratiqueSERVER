plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.6")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("com.google.guava:guava:28.0-jre")
    implementation("org.json:json:20180813")
    implementation("log4j:log4j:1.2.17")

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}