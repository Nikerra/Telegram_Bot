FROM respo/jdk20
WORKDIR /app
COPY application.yml /app
COPY Telegram_Bot-0.0.1-SNAPSHOT.jar /app
ENTRYPOINT ["java", "-jar", "/app/Telegram_Bot-0.0.1-SNAPSHOT.jar"]
