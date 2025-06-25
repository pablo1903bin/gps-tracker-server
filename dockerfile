# Usa una imagen base con Java 17
FROM eclipse-temurin:17-jdk

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo JAR generado al contenedor
COPY target/app.jar app.jar

# Expone el puerto en el que se ejecuta la aplicación
EXPOSE 5055

# Comando por default
ENTRYPOINT ["java", "-jar", "app.jar"]
