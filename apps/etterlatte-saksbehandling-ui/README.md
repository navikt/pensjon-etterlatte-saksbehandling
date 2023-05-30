# Etterlatte-saksbehandling-ui

Appen består av en React frontend skrevet i Typescript og en backend-for-frontend med Express.

Når appen kjøres opp lokalt settes det også opp en mock-router som server statiske JSON for forskjellige behandlinger 
for testing.

---

test

## Kjøring lokalt

Avhengigheter må installeres for at prosjektet skal kjøre. Dette kan du gjøre ved å kjøre kommandoen `yarn` i enten 
ui sin rot, eller i [client](./client) og [server](./server) individuelt.

Kjøres fra mappen `client` og/eller `server`. \
Prosjektet bruker [Vite](https://vitejs.dev/).

Det finnes to måter å kjøre opp frontend:

### Alternativ 1: Ren mock _uten_ auth og docker

Med ren mock vil frontend bruke lokale mockfiler for å simulere backend APIer. 

Kjøres opp med: 
1. `yarn dev` i både [client](./client) og [server](./server).
2. Frontend er nå tilgjengelig på [localhost:5173](http://localhost:5173)


### Alternativ 2: _Med_ auth og docker

For å kjøre mot backend lokalt med docker må det være mulig å nå `host.docker.internal`, så pass på at du legger til 
dette i `/etc/hosts`:

`127.0.0.1 host.docker.internal`


#### Variant A: Med gyldig token mot dev-gcp

For å kjøre mot dev-gcp må du først kjøre `get-secret.sh` ([../../get-secret.sh](../../get-secret.sh)). \
Felles bruksanvisning til scriptet finner du på [prosjektets rot](../..). 

Scriptet henter ut secrets for lokal kjøring mot dev-gcp og lagrer de i en lokal fil (`.env.dev-gcp`). 
Denne filen skal inneholde en rekke miljøvariabler som brukes av frackend og wonderwall. 

Om du ønsker å gå mot en backend-app lokalt kan du legge til miljøvariabler for dette i `.env.dev-gcp`. \
Se i filen [config.ts](./server/src/config/config.ts) for å se gyldige nøkler. Trenger kun legge til de 
du skal benytte deg av lokalt. Resten vil automatisk gå mot dev.

Når `.env.dev-gcp` er opprettet kan du kjøre opp docker (dev-gcp varianten):

1. Kjør `docker-compose -f ./docker-compose.dev-gcp.yml up -d` \
   Dette starter wonderwall og frackend i docker, _med_ gyldig AzureAD config.
2. Start frontend (client):
   `yarn dev`
3. Du når frontend *via* wonderwall på [localhost:3000](http://localhost:3000) \
   Frontend kjører nå mot autentisert frackend og har gyldig Authorization i header.

**NB:** Hvis du gjør endringer i frackend kan det hende du må bygge den på nytt. Kan gjøres ved å legge til
`--build --force-recreate` i kommandoen over.


#### Variant B: Med mock-oauth2 mot lokal backend

For å kjøre frackend med mock-oauth2 må du lage en fil som heter `.env`. I den filen kan du legge til lenke til 
backend API som skal testes. Nøkler/endepunkter som ikke er spesifisert vil returnere mockdata.\
Gyldige nøkler ser du i `config.ts`

1. Kjør `docker-compose up -d` \
   Dette starter wonderwall, mock-oauth2-server, og frackend i docker.
2. Start frontend (client):
   `yarn dev`
3. Du når frontend *via* wonderwall på [localhost:3000](http://localhost:3000) \
   Frontend kjører nå mot autentisert frackend og har gyldig Authorization i header.


---

## Bygg og deploy
Bygg og deploy kjøres via github workflows i roten på prosjektet
og kjører bygg av både client og server folder. \
Se Dockerfile for oppsett av container-image