#!/bin/bash

./gradlew publishToMavenLocal -Pversion=latest-SNAPSHOT

cd test-groovy \
 && ./gradlew uD \
 && ./gradlew build \
 && cd -

cd test-kotlin \
 && ./gradlew uD \
 && ./gradlew build \
 && cd -

git diff

git checkout HEAD test-groovy test-kotlin
