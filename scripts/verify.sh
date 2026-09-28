#!/bin/sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/learnpay-email-classes"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
javac --release 17 -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java' | sort)
java -cp "$BUILD_DIR" learnpay.email.service.LifecycleNotificationServiceTest
