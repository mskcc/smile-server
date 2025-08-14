#!/bin/sh
set -e
"${SMILE_CONFIG_HOME}/scripts/saml2aws-setup.sh"
exec java -jar /smile-server/smile_server.jar
