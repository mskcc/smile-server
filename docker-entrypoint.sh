#!/bin/sh
set -e
"${SMILE_CONFIG_HOME}/scripts/saml2aws-smile-setup.sh"
exec java --add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED -Dspring.main.allow-circular-references=true -jar /smile-server/smile_server.jar
