# Etterlatte-saksbehandling-ui


Appen består av en statisk frontend og en backend-for-frontends i NodeJS.


## Kjøring lokalt
### Client

**`yarn start`**

Starter frontend-appen på localhost:3000

### Server
**`yarn dev`**

Kjører opp app serveren på port 8080 eller den porten som er angitt via `process.env.PORT`



## Bygg og deploy
Bygg og deploy kjøres via github workflows i roten på prosjektet
og kjører bygg av både client og server folder. \
Se Dockerfile for oppsett av container-image