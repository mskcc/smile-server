#!/bin/sh
set -e
"${SMILE_CONFIG_HOME}/scripts/saml2aws-setup.sh"
# Then execute the main application (your Java JAR)
exec java -jar /smile-server/smile_server.jar
