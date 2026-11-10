#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: scripts/run-example.sh learner@example.com" >&2
  exit 2
fi

BUILD_DIR="${TMPDIR:-/tmp}/learnpay-email-example"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
javac --release 17 -d "$BUILD_DIR" $(find src/main/java -name '*.java' | sort)
java -cp "$BUILD_DIR" learnpay.email.PaymentReceiptExample "$1"
