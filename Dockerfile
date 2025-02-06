FROM bitnami/nginx:1.27.3

WORKDIR /app

COPY ./src .
