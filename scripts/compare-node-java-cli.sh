#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
UPSTREAM_DIR="${MIKU_DOCX2MD_UPSTREAM_DIR:-${ROOT_DIR}/../miku-docx2md}"
WORK_DIR="${ROOT_DIR}/workplace/node-java-cli"
JAVA_JAR="${ROOT_DIR}/target/miku-docx2md-1.0.0.jar"

(cd "${ROOT_DIR}" && mvn -q -DskipTests package)

if [ ! -f "${UPSTREAM_DIR}/scripts/miku-docx2md-cli.mjs" ]; then
  echo "upstream CLI not found: ${UPSTREAM_DIR}" >&2
  exit 1
fi

mkdir -p "${WORK_DIR}/node" "${WORK_DIR}/java"

compare_file() {
  label="$1"
  node_file="$2"
  java_file="$3"
  diff_file="$4"
  if diff -u "${node_file}" "${java_file}" > "${diff_file}"; then
    return 0
  fi
  echo "${label} diff: ${diff_file}" >&2
  return 1
}

compare_status() {
  label="$1"
  node_status="$2"
  java_status="$3"
  status_file="$4"
  if [ "${node_status}" = "${java_status}" ]; then
    return 0
  fi
  {
    echo "node ${node_status}"
    echo "java ${java_status}"
  } > "${status_file}"
  echo "${label} status diff: ${status_file}" >&2
  return 1
}

normalize_help() {
  sed 's#node scripts/miku-docx2md-cli.mjs#java -jar miku-docx2md-1.0.0.jar#g'
}

normalize_version() {
  sed 's/^miku-docx2md .*/miku-docx2md <version>/'
}

normalize_java_help() {
  awk '/^JAVA EXTENSIONS$/ { skip = 1; next } /^OUTPUTS$/ { skip = 0 } !skip { print }'
}

normalize_verbose() {
  sed -E "s#${WORK_DIR}/(node|java)#${WORK_DIR}/<side>#g; s/verbose: \+[0-9]+ms/verbose: +<ms>ms/g; s/done total-ms=[0-9]+/done total-ms=<ms>/g"
}

status=0
compare_metadata_status=0

(cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs --version) | normalize_version > "${WORK_DIR}/node/version.stdout"
java -jar "${JAVA_JAR}" --version | normalize_version > "${WORK_DIR}/java/version.stdout"
compare_file "version stdout" "${WORK_DIR}/node/version.stdout" "${WORK_DIR}/java/version.stdout" "${WORK_DIR}/version.stdout.diff" || compare_metadata_status=1

(cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs --help) | normalize_help > "${WORK_DIR}/node/help.stdout"
java -jar "${JAVA_JAR}" --help | normalize_java_help > "${WORK_DIR}/java/help.stdout"
compare_file "help stdout" "${WORK_DIR}/node/help.stdout" "${WORK_DIR}/java/help.stdout" "${WORK_DIR}/help.stdout.diff" || compare_metadata_status=1

set +e
(cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs > "${WORK_DIR}/node/missing-input.stdout" 2> "${WORK_DIR}/node/missing-input.stderr")
node_missing_status=$?
java -jar "${JAVA_JAR}" > "${WORK_DIR}/java/missing-input.raw.stdout" 2> "${WORK_DIR}/java/missing-input.stderr"
java_missing_status=$?
(cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs sample.docx --unknown > "${WORK_DIR}/node/unknown-option.stdout" 2> "${WORK_DIR}/node/unknown-option.stderr")
node_unknown_status=$?
java -jar "${JAVA_JAR}" sample.docx --unknown > "${WORK_DIR}/java/unknown-option.stdout" 2> "${WORK_DIR}/java/unknown-option.stderr"
java_unknown_status=$?
(cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs sample.docx --out > "${WORK_DIR}/node/missing-option-value.stdout" 2> "${WORK_DIR}/node/missing-option-value.stderr")
node_missing_value_status=$?
java -jar "${JAVA_JAR}" sample.docx --out > "${WORK_DIR}/java/missing-option-value.stdout" 2> "${WORK_DIR}/java/missing-option-value.stderr"
java_missing_value_status=$?
set -e

normalize_help < "${WORK_DIR}/node/missing-input.stdout" > "${WORK_DIR}/node/missing-input.normalized.stdout"
mv "${WORK_DIR}/node/missing-input.normalized.stdout" "${WORK_DIR}/node/missing-input.stdout"
normalize_java_help < "${WORK_DIR}/java/missing-input.raw.stdout" > "${WORK_DIR}/java/missing-input.stdout"

compare_status "missing input" "${node_missing_status}" "${java_missing_status}" "${WORK_DIR}/missing-input.status.diff" || compare_metadata_status=1
compare_file "missing input stdout" "${WORK_DIR}/node/missing-input.stdout" "${WORK_DIR}/java/missing-input.stdout" "${WORK_DIR}/missing-input.stdout.diff" || compare_metadata_status=1
compare_file "missing input stderr" "${WORK_DIR}/node/missing-input.stderr" "${WORK_DIR}/java/missing-input.stderr" "${WORK_DIR}/missing-input.stderr.diff" || compare_metadata_status=1
compare_status "unknown option" "${node_unknown_status}" "${java_unknown_status}" "${WORK_DIR}/unknown-option.status.diff" || compare_metadata_status=1
compare_file "unknown option stdout" "${WORK_DIR}/node/unknown-option.stdout" "${WORK_DIR}/java/unknown-option.stdout" "${WORK_DIR}/unknown-option.stdout.diff" || compare_metadata_status=1
compare_file "unknown option stderr" "${WORK_DIR}/node/unknown-option.stderr" "${WORK_DIR}/java/unknown-option.stderr" "${WORK_DIR}/unknown-option.stderr.diff" || compare_metadata_status=1
compare_status "missing option value" "${node_missing_value_status}" "${java_missing_value_status}" "${WORK_DIR}/missing-option-value.status.diff" || compare_metadata_status=1
compare_file "missing option value stdout" "${WORK_DIR}/node/missing-option-value.stdout" "${WORK_DIR}/java/missing-option-value.stdout" "${WORK_DIR}/missing-option-value.stdout.diff" || compare_metadata_status=1
compare_file "missing option value stderr" "${WORK_DIR}/node/missing-option-value.stderr" "${WORK_DIR}/java/missing-option-value.stderr" "${WORK_DIR}/missing-option-value.stderr.diff" || compare_metadata_status=1
if [ "${compare_metadata_status}" -eq 0 ]; then
  echo "ok cli-metadata"
else
  status=1
fi

if [ "$#" -eq 0 ]; then
  set -- "${ROOT_DIR}"/src/test/resources/docx/*.docx
fi

for input in "$@"; do
  name=$(basename -- "${input}" .docx)
  node_markdown_output="${WORK_DIR}/node/${name}.md"
  java_markdown_output="${WORK_DIR}/java/${name}.md"
  node_summary_output="${WORK_DIR}/node/${name}.summary.txt"
  java_summary_output="${WORK_DIR}/java/${name}.summary.txt"
  node_stdout_output="${WORK_DIR}/node/${name}.stdout.txt"
  java_stdout_output="${WORK_DIR}/java/${name}.stdout.txt"
  node_summary_stdout_output="${WORK_DIR}/node/${name}.summary-stdout.txt"
  java_summary_stdout_output="${WORK_DIR}/java/${name}.summary-stdout.txt"
  node_debug_output="${WORK_DIR}/node/${name}.debug.md"
  java_debug_output="${WORK_DIR}/java/${name}.debug.md"
  node_include_unsupported_output="${WORK_DIR}/node/${name}.include-unsupported.md"
  java_include_unsupported_output="${WORK_DIR}/java/${name}.include-unsupported.md"
  node_verbose_output="${WORK_DIR}/node/${name}.verbose.md"
  java_verbose_output="${WORK_DIR}/java/${name}.verbose.md"
  node_verbose_stderr="${WORK_DIR}/node/${name}.verbose.stderr"
  java_verbose_stderr="${WORK_DIR}/java/${name}.verbose.stderr"
  node_assets_dir="${WORK_DIR}/node/${name}.assets"
  java_assets_dir="${WORK_DIR}/java/${name}.assets"
  node_verbose_assets_dir="${WORK_DIR}/node/${name}.verbose.assets"
  java_verbose_assets_dir="${WORK_DIR}/java/${name}.verbose.assets"

  rm -rf "${node_assets_dir}" "${java_assets_dir}" "${node_verbose_assets_dir}" "${java_verbose_assets_dir}"
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" --out "${node_markdown_output}" --summary-out "${node_summary_output}" --assets-dir "${node_assets_dir}")
  java -jar "${JAVA_JAR}" "${input}" --out "${java_markdown_output}" --summary-out "${java_summary_output}" --assets-dir "${java_assets_dir}"
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" > "${node_stdout_output}")
  java -jar "${JAVA_JAR}" "${input}" > "${java_stdout_output}"
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" --summary > "${node_summary_stdout_output}")
  java -jar "${JAVA_JAR}" "${input}" --summary > "${java_summary_stdout_output}"
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" --out "${node_debug_output}" --debug)
  java -jar "${JAVA_JAR}" "${input}" --out "${java_debug_output}" --debug
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" --out "${node_include_unsupported_output}" --include-unsupported-comments)
  java -jar "${JAVA_JAR}" "${input}" --out "${java_include_unsupported_output}" --include-unsupported-comments
  (cd "${UPSTREAM_DIR}" && node scripts/miku-docx2md-cli.mjs "${input}" --out "${node_verbose_output}" --summary-out "${WORK_DIR}/node/${name}.verbose.summary.txt" --assets-dir "${node_verbose_assets_dir}" --verbose 2> "${node_verbose_stderr}")
  java -jar "${JAVA_JAR}" "${input}" --out "${java_verbose_output}" --summary-out "${WORK_DIR}/java/${name}.verbose.summary.txt" --assets-dir "${java_verbose_assets_dir}" --verbose 2> "${java_verbose_stderr}"
  normalize_verbose < "${node_verbose_stderr}" > "${node_verbose_stderr}.normalized"
  mv "${node_verbose_stderr}.normalized" "${node_verbose_stderr}"
  normalize_verbose < "${java_verbose_stderr}" > "${java_verbose_stderr}.normalized"
  mv "${java_verbose_stderr}.normalized" "${java_verbose_stderr}"

  markdown_status=0
  summary_status=0
  stdout_status=0
  summary_stdout_status=0
  debug_status=0
  include_unsupported_status=0
  verbose_status=0
  assets_status=0
  diff -u "${node_markdown_output}" "${java_markdown_output}" > "${WORK_DIR}/${name}.markdown.diff" || markdown_status=$?
  diff -u "${node_summary_output}" "${java_summary_output}" > "${WORK_DIR}/${name}.summary.diff" || summary_status=$?
  diff -u "${node_stdout_output}" "${java_stdout_output}" > "${WORK_DIR}/${name}.stdout.diff" || stdout_status=$?
  diff -u "${node_summary_stdout_output}" "${java_summary_stdout_output}" > "${WORK_DIR}/${name}.summary-stdout.diff" || summary_stdout_status=$?
  diff -u "${node_debug_output}" "${java_debug_output}" > "${WORK_DIR}/${name}.debug.diff" || debug_status=$?
  diff -u "${node_include_unsupported_output}" "${java_include_unsupported_output}" > "${WORK_DIR}/${name}.include-unsupported.diff" || include_unsupported_status=$?
  diff -u "${node_verbose_stderr}" "${java_verbose_stderr}" > "${WORK_DIR}/${name}.verbose.stderr.diff" || verbose_status=$?
  diff -ru "${node_assets_dir}" "${java_assets_dir}" > "${WORK_DIR}/${name}.assets.diff" || assets_status=$?

  if [ "${markdown_status}" -eq 0 ] && [ "${summary_status}" -eq 0 ] && [ "${stdout_status}" -eq 0 ] && [ "${summary_stdout_status}" -eq 0 ] && [ "${debug_status}" -eq 0 ] && [ "${include_unsupported_status}" -eq 0 ] && [ "${verbose_status}" -eq 0 ] && [ "${assets_status}" -eq 0 ]; then
    echo "ok ${name}"
  else
    if [ "${markdown_status}" -ne 0 ]; then
      echo "markdown diff ${name}: ${WORK_DIR}/${name}.markdown.diff" >&2
    fi
    if [ "${summary_status}" -ne 0 ]; then
      echo "summary diff ${name}: ${WORK_DIR}/${name}.summary.diff" >&2
    fi
    if [ "${stdout_status}" -ne 0 ]; then
      echo "stdout diff ${name}: ${WORK_DIR}/${name}.stdout.diff" >&2
    fi
    if [ "${summary_stdout_status}" -ne 0 ]; then
      echo "summary stdout diff ${name}: ${WORK_DIR}/${name}.summary-stdout.diff" >&2
    fi
    if [ "${debug_status}" -ne 0 ]; then
      echo "debug diff ${name}: ${WORK_DIR}/${name}.debug.diff" >&2
    fi
    if [ "${include_unsupported_status}" -ne 0 ]; then
      echo "include unsupported diff ${name}: ${WORK_DIR}/${name}.include-unsupported.diff" >&2
    fi
    if [ "${verbose_status}" -ne 0 ]; then
      echo "verbose stderr diff ${name}: ${WORK_DIR}/${name}.verbose.stderr.diff" >&2
    fi
    if [ "${assets_status}" -ne 0 ]; then
      echo "assets diff ${name}: ${WORK_DIR}/${name}.assets.diff" >&2
    fi
    status=1
  fi
done

exit "${status}"
