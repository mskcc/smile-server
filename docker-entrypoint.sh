#!/bin/sh
set -e
"${SMILE_CONFIG_HOME}/scripts/saml2aws-smile-setup.sh"
exec java -Dspring.main.allow-circular-references=true -jar /smile-server/smile_server.jar
