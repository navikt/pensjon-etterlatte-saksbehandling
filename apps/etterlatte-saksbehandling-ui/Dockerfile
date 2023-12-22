FROM node:20-bookworm-slim

WORKDIR /app
COPY ./client/dist ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

ENV NODE_ENV="production"

RUN cd ./server && yarn install --frozen-lockfile

EXPOSE 4000

CMD ["node", "./server/index.js"]
