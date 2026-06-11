FROM eclipse-temurin:21-jdk AS build

# Credentials for the 'github' Maven repository declared in pom.xml.
# Used to resolve com.open-elements:spring-services:1.0.0-SNAPSHOT from
# GitHub Packages (spec 107). Pass via:
#   docker build --build-arg GITHUB_ACTOR=$USER --build-arg GITHUB_TOKEN=$TOKEN ...
# Defaults are empty so cached builds that don't need the snapshot keep working.
ARG GITHUB_ACTOR=""
ARG GITHUB_TOKEN=""
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -s .mvn/settings.xml dependency:go-offline -B

COPY src/ src/
RUN ./mvnw -s .mvn/settings.xml clean package -DskipTests -B

# Debian-based runtime (eclipse-temurin:21-jre) because libheif is required for
# HEIC decoding (spec 102). Alpine does not ship libheif-plugin-libde265 in a
# usable form for libheif's plugin discovery.
FROM eclipse-temurin:21-jre

# Native libraries for HEIC decoding via NightMonkeys imageio-heif.
# libheif1                 -> the shared library
# libheif-plugin-libde265  -> the HEVC decoder plugin (required to decode HEIC)
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libheif1 \
        libheif-plugin-libde265 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# --enable-native-access=ALL-UNNAMED silences the JDK 21 warning about the
# Foreign Function & Memory API used by NightMonkeys to call into libheif.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
