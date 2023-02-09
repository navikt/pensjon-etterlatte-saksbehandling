FROM cypress/included:9.4.1

WORKDIR /home/node/app

COPY ./cypress ./cypress
COPY ./cypress/e2e ./cypress/integration
COPY cypress.config.ts package.json ./

RUN npm install
ENTRYPOINT npx cypress run