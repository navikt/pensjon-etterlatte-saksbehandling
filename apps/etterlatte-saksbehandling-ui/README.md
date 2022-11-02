# Etterlatte-saksbehandling-ui


Appen består av en React frontend skrevet i Typescript og en backend-for-frontend med Express.

Når appen kjøres opp lokalt settes det også opp en mock-router som server statiske JSON for forskjellige behandlinger 
for testing.

## Kjøring lokalt

### Installering av avhengigheter
Kjør `yarn` i denne mappen.

### Client
Kjøres fra mappen `client`. Frontendend startes med kommandoen

**`yarn start`**

Frontend-appen kjører på `localhost:3000`

### Server
Kjøres fra mappen `server` med kommandoen

**`yarn dev`**

Serveren på port `8080` eller den porten som er angitt via `process.env.PORT`


## Miljøvariabler for lokal kjøring
`BREV_DEV` kan settes til `true` hvis man vil gå mot en lokal instans av `etterlatte-brev-api`.
`VILKAARSVURDERING_DEV` kan settes til `true` hvis man vil gå mot en lokal instans av `etterlatte-vilkaarsvurdering`.


## Bygg og deploy
Bygg og deploy kjøres via github workflows i roten på prosjektet
og kjører bygg av både client og server folder. \
Se Dockerfile for oppsett av container-image