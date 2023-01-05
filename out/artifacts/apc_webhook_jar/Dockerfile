FROM openjdk:11
COPY apc-webhook.jar .
RUN mkdir /tokens
COPY tokens/StoredCredential tokens/StoredCredential
CMD ["java","-jar","apc-webhook.jar"]