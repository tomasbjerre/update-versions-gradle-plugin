plugins {
	id("java-library")
	id("se.bjurr.gradle.update-versions") version "latest-SNAPSHOT"
	id("se.bjurr.gitchangelog.git-changelog-gradle-plugin") version "3.0.0" apply false
}

dependencies {
	/**
	 * These should be updated by the plugin.
	 */
	implementation("se.bjurr.violations:violations-lib:1.0.0")
}

repositories {
	gradlePluginPortal()
	mavenCentral()
	mavenLocal()
}
