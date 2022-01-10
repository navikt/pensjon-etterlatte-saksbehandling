FROM node:16-alpine3.14


WORKDIR /app
COPY ./client/build ./client
COPY ./server/build ./server
COPY ./server/package.json ./server

RUN cd ./server && npm install --production

EXPOSE 4000

CMD ["NODE_ENV=production", "node", "./server/index.js"]
