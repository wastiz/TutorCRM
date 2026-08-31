# ---- build ----
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- serve ----
FROM nginx:1.27-alpine AS runtime
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# BACKEND_URL is substituted into the nginx config at container start by the
# stock nginx entrypoint (docker-entrypoint.d/20-envsubst-on-templates.sh).
ENV BACKEND_URL=http://backend:8080
ENV PORT=8080
EXPOSE 8080
