FROM bellsoft/liberica-openjre-alpine:21-cds

WORKDIR /home/app

COPY ./jar/*.jar /home/app/app.jar

ENTRYPOINT ["java","-jar","app.jar"]