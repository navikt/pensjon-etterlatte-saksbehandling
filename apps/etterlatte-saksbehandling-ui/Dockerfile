FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim as base

WORKDIR /app
COPY ./client/dist ./client
COPY ./server/build ./server/package.json ./server/yarn.lock ./server/

ENV NODE_ENV="production"

RUN cd ./server && yarn install --frozen-lockfile


FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24 as prod
COPY --from=base /app /app

USER nonroot
ENV NODE_ENV="production"
EXPOSE 4000

CMD ["./server/index.js"]