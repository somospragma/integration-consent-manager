FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Integration Chapter - Pragma"
LABEL service="ms-notification-dispatcher"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY build/libs/ms-notification-dispatcher-*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
