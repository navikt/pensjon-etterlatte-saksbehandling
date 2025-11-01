FROM node:25-bookworm-slim as base

WORKDIR /app
COPY ./client/dist ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

ENV NODE_ENV="production"

RUN cd ./server && yarn install --frozen-lockfile


FROM gcr.io/distroless/nodejs24-debian12 as prod
COPY --from=base /app /app

USER nonroot
ENV NODE_ENV="production"

WORKDIR /app
EXPOSE 4000

CMD ["./server/index.js"]
