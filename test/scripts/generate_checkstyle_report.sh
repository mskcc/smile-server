#!/bin/bash

export REPO_TMP_DIR="$1"
if [ -z "$REPO_TMP_DIR" ] ; then
    echo "Missing repository path: generate_checkstyle_report [path to repository]"
    exit 1
fi

find $REPO_TMP_DIR/* -name checkstyle_report.txt -print | xargs grep -v -E "^Starting audit|^Audit done" > $REPO_TMP_DIR/checkstyle_report_compiled.txt
if [[ $(wc -l < $REPO_TMP_DIR/checkstyle_report_compiled.txt) -gt 0 ]]; then
    echo "checkstyle plugin found issues, the compiled report can be found at /checkstyle_report.txt"
    exit 1
fi
exit 0
