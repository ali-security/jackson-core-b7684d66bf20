# Sourced from .travis.yml before_install (do not use `set -e`: it would kill the shell).
seal_fetch_jdk() {
  # $1 = Zulu archive name, $2 = install dir
  echo "Installing $1 into $2"
  mkdir -p "$2" || return 1
  curl -fsSL --retry 5 --retry-delay 10 -o /tmp/seal-jdk.tar.gz "https://cdn.azul.com/zulu/bin/$1.tar.gz" || return 1
  tar -xzf /tmp/seal-jdk.tar.gz -C "$2" --strip-components=1 || return 1
}
SEAL_MVN_ARGS=""
if [ -n "$SEAL_JDK" ]; then
  seal_fetch_jdk "$SEAL_JDK" /tmp/seal-jdk || return 1
  # Zulu 7 ships an old trust store; reuse the image's JDK 8 cacerts for Maven Central TLS.
  cp "$JAVA_HOME/jre/lib/security/cacerts" /tmp/seal-jdk/jre/lib/security/cacerts || return 1
  export JAVA_HOME=/tmp/seal-jdk
  export PATH="$JAVA_HOME/bin:$PATH"
  export MAVEN_OPTS="-Dhttps.protocols=TLSv1.2"
fi
if [ -n "$SEAL_TEST_JDK" ]; then
  seal_fetch_jdk "$SEAL_TEST_JDK" /tmp/seal-test-jdk || return 1
  /tmp/seal-test-jdk/bin/java -version || return 1
  # Surefire forks the test JVM from this binary; Maven itself stays on JDK 8.
  # -showversion makes the forked test JVM print its own version banner into the log.
  SEAL_MVN_ARGS="-Djvm=/tmp/seal-test-jdk/bin/java -DargLine=-showversion"
fi
export SEAL_MVN_ARGS
echo "Extra surefire args for this leg: ${SEAL_MVN_ARGS:-(none)}"
# Travis's preinstalled Maven 3.9 needs Java 8; Maven 3.8.8 still runs on Java 7.
for URL in https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.tar.gz \
           https://archive.apache.org/dist/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz; do
  curl -fsSL --retry 5 --retry-delay 10 -o /tmp/maven.tar.gz "$URL" && tar -xzf /tmp/maven.tar.gz -C /tmp && break
done
[ -x /tmp/apache-maven-3.8.8/bin/mvn ] || return 1
export M2_HOME=/tmp/apache-maven-3.8.8
export PATH="$M2_HOME/bin:$PATH"
java -version
mvn -version
