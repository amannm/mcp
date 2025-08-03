#!/usr/bin/env bash
set -euo pipefail

# Version variables
GRAALVM_VERSION=24
GRAALVM_PATCH_VERSION=24.0.1
MAVEN_VERSION=3.9.10
GRADLE_VERSION=8.14.3

# setup graalvm
GRAALVM_HOME=/opt/graalvm-jdk-${GRAALVM_VERSION}
if [[ ! -x "$GRAALVM_HOME/bin/java" ]]; then
  sudo mkdir -p "$GRAALVM_HOME"
  curl -L https://download.oracle.com/graalvm/${GRAALVM_VERSION}/archive/graalvm-jdk-${GRAALVM_PATCH_VERSION}_linux-x64_bin.tar.gz \
    | sudo tar -xz --strip-components=1 -C "$GRAALVM_HOME" -f -
fi
export GRAALVM_HOME
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$HOME/bin:$PATH"
if [[ ! -s "$GRAALVM_HOME/lib/security/cacerts" && -f /etc/ssl/certs/java/cacerts ]]; then
  sudo cp /etc/ssl/certs/java/cacerts "$GRAALVM_HOME/lib/security/cacerts"
fi
if [[ -n "${CODEX_PROXY_CERT:-}" && -f "${CODEX_PROXY_CERT}" ]]; then
  if ! "$GRAALVM_HOME/bin/keytool" -list \
        -keystore "$GRAALVM_HOME/lib/security/cacerts" \
        -storepass changeit -alias codex-proxy >/dev/null 2>&1; then
    sudo "$GRAALVM_HOME/bin/keytool" -importcert -noprompt \
      -alias codex-proxy \
      -file "${CODEX_PROXY_CERT}" \
      -keystore "$GRAALVM_HOME/lib/security/cacerts" \
      -storepass changeit
  fi
fi

# setup maven
if [[ ! -x /usr/local/bin/mvn ]]; then
  curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | sudo tar -xz -C /opt
  sudo ln -sf /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn
fi
mkdir -p "${HOME}/.m2"
cat > "${HOME}/.m2/settings.xml" <<'EOF'
<settings>
  <proxies>
    <proxy>
      <id>codexHttpProxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy</host>
      <port>8080</port>
    </proxy>
    <proxy>
      <id>codexHttpsProxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>proxy</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
EOF

# setup gradle
if [[ ! -x /usr/local/bin/gradle ]]; then
  curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o /tmp/gradle.zip
  sudo unzip -q /tmp/gradle.zip -d /opt
  sudo ln -sf /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle
  rm /tmp/gradle.zip
fi

mkdir -p "${HOME}/.gradle"
cat > "${HOME}/.gradle/gradle.properties" <<EOF
systemProp.http.proxyHost=proxy
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy
systemProp.https.proxyPort=8080
org.gradle.java.installations.auto-detect=false
org.gradle.java.installations.paths=${GRAALVM_HOME}
EOF

chmod 444 ./specification/**/*.*

