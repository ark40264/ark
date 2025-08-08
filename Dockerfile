FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY app.jar /app/app.jar
COPY sqlite.db /app/sqlite.db
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]

