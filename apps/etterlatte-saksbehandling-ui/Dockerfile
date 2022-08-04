FROM node:16-alpine3.14


WORKDIR /app
COPY ./client/build ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

ENV NODE_ENV="production"

RUN cd ./server && yarn install --frozen-lockfile

EXPOSE 4000

CMD ["node", "./server/index.js"]
