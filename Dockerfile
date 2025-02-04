FROM bitnami/nginx:1.25.1

WORKDIR /app

COPY ./src .
