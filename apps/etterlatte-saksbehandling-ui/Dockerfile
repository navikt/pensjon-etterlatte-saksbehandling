FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-dev as base

USER root
WORKDIR /app
COPY ./client/dist ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

ENV NODE_ENV="production"

RUN cd ./server && yarn install --frozen-lockfile


FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim as prod
COPY --from=base /app /app

USER nonroot
ENV NODE_ENV="production"

WORKDIR /app
EXPOSE 4000

CMD ["./server/index.js"]
