#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK_DIR="$ROOT_DIR/target/maven-plugin-smoke"
INPUT_FILE="$ROOT_DIR/miku-docx2md/src/test/resources/docx/word-headings-basic.docx"
DIR_SOURCE_FILE="$ROOT_DIR/miku-docx2md/src/test/resources/docx/word-bullet-list-basic.docx"
OUTPUT_FILE="$WORK_DIR/word-headings-basic.md"
SUMMARY_FILE="$WORK_DIR/word-headings-basic.summary.txt"
DIR_INPUT_DIR="$WORK_DIR/directory-input"
DIR_OUTPUT_DIR="$WORK_DIR/directory-output"

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR" "$DIR_INPUT_DIR"

cp "$DIR_SOURCE_FILE" "$DIR_INPUT_DIR/word-bullet-list-basic.docx"

( cd "$ROOT_DIR" && mvn -DskipTests install )
( cd "$ROOT_DIR" && mvn -N jp.igapyon:miku-docx2md-maven-plugin:0.9.0:convert \
  -Dmiku-docx2md.inputFile="$INPUT_FILE" \
  -Dmiku-docx2md.outputFile="$OUTPUT_FILE" \
  -Dmiku-docx2md.summaryFile="$SUMMARY_FILE" \
  -Dmiku-docx2md.includeUnsupportedComments=true \
  -Dmiku-docx2md.verbose=true )

test -f "$OUTPUT_FILE"
test -f "$SUMMARY_FILE"
grep -Fq "# Heading 1" "$OUTPUT_FILE"
grep -Fq "headings: 6" "$SUMMARY_FILE"

( cd "$ROOT_DIR" && mvn -N jp.igapyon:miku-docx2md-maven-plugin:0.9.0:convert-directory \
  -Dmiku-docx2md.inputDirectory="$DIR_INPUT_DIR" \
  -Dmiku-docx2md.outputDirectory="$DIR_OUTPUT_DIR" \
  -Dmiku-docx2md.recursive=false \
  -Dmiku-docx2md.verbose=true )

test -f "$DIR_OUTPUT_DIR/word-bullet-list-basic.md"
grep -Fq "Bullet" "$DIR_OUTPUT_DIR/word-bullet-list-basic.md"

printf '%s\n' "Maven plugin smoke passed: target/maven-plugin-smoke/word-headings-basic.md"
printf '%s\n' "Maven plugin directory smoke passed: target/maven-plugin-smoke/directory-output/word-bullet-list-basic.md"
