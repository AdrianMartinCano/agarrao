# syntax=docker/dockerfile:1
### Etapa 1: build de la PWA con Angular ###
FROM node:22-alpine AS build
WORKDIR /app

ENV CI=true
ENV NG_CLI_ANALYTICS=false

# El npm que generó el package-lock.json, no el de la imagen. Ojo: npm 11.5.1 falla al
# instalar este proyecto con "Cannot read properties of null (reading 'edgesOut')" (fallo
# de npm con los peers de vitest/jsdom); con 11.21.0 va bien.
RUN npm install -g npm@11.21.0

# Token de GitHub Packages para @adrianmartincano/ng-*. El .npmrc lo lee del entorno.
# OJO Coolify: GITHUB_TOKEN tiene que ser "Build Variable" o llega vacío.
ARG GITHUB_TOKEN
RUN test -n "$GITHUB_TOKEN" || { echo "ERROR: GITHUB_TOKEN vacío en el build del front. Márcalo como Build Variable en Coolify."; exit 1; }
ENV GITHUB_TOKEN=$GITHUB_TOKEN

COPY package*.json .npmrc ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
# Que el token no se quede en ninguna capa que se reutilice.
RUN rm -f .npmrc

COPY . .
RUN npm run build   # prebuild escribe la versión desde package.json

### Etapa 2: nginx sirve la PWA y hace de proxy a /api ###
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/front/browser /usr/share/nginx/html
EXPOSE 80
