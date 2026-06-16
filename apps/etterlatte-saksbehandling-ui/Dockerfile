FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim

WORKDIR /app
COPY ./client/dist ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

USER nonroot
ENV NODE_ENV="production"
EXPOSE 4000

CMD ["./server/index.js"]