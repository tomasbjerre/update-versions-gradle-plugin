#!/bin/bash

./gradlew publishToMavenLocal -Pversion=latest-SNAPSHOT

cd test-groovy \
 && ./gradlew sUD -s \
 && ./gradlew uD -s \
 && ./gradlew build \
 && cd -

cd test-kotlin \
 && ./gradlew sUD -s \
 && ./gradlew uD -s \
 && ./gradlew build \
 && cd -

git diff | cat

git checkout HEAD test-groovy test-kotlin
