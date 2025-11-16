plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.5")
    implementation("org.apache.commons:commons-lang3:3.8")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("joda-time:joda-time:2.9.9")

    testImplementation(platform("org.junit:junit-bom:5.6.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}