#!/bin/bash

METADB_TMP_DIR=/tmp/repos/cmo-metadb

find $METADB_TMP_DIR/* -name checkstyle_report.txt -print | xargs grep -v -E "^Starting audit|^Audit done" > $METADB_TMP_DIR/checkstyle_report_compiled.txt
if [[ $(wc -l < $METADB_TMP_DIR/checkstyle_report_compiled.txt) -gt 0 ]]; then
    echo "checkstyle plugin found issues, the compiled report can be found at /checkstyle_report.txt"
    exit 1
fi
exit 0
