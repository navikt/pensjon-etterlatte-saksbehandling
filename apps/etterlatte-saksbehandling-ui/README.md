# Etterlatte-saksbehandling-ui

Appen består av en React frontend skrevet i Typescript og en backend-for-frontend med Express.

Når appen kjøres opp lokalt settes det også opp en mock-router som server statiske JSON for forskjellige behandlinger 
for testing.

## Kjøring lokalt

Avhengigheter må installeres for at prosjektet skal kjøre. Dette kan du gjøre ved å kjøre kommandoen `yarn` i enten 
ui sin rot, eller i [client](./client) og [server](./server) individuelt.

Det finnes to måter å kjøre opp frontend:

### Alt. 1: Ren mock

Med ren mock vil frontend bruke lokale mockfiler for å simulere backend APIer. 

Kjøres opp med: 
1. `yarn dev` i både [client](./client) og [server](./server).
2. Frontend er nå tilgjengelig på [localhost:5173](http://localhost:5173)


### Alt. 2: Mot lokal backend (med auth)

For å kjøre mot backend lokalt må du koble til wonderwall og mock-oauth2-server. Dette gjøres automatisk ved å 
kjøre docker-compose, men først les biten under. 

**Steg 1:** Det må være mulig å nå `host.docker.internal`, så pass på at du legger til dette i `/etc/hosts`:

`127.0.0.1 host.docker.internal`

**Steg 2:** Opprett en fil som heter `.env` (i samme mappe som denne README-filen). Filen skal inneholde følgende variabler. 
Dersom du ikke oppgir en API_URL vil server defaulte til mock-data. API_URL brukes ved eksempelvis kjøring av 
APIet (backend) lokalt sammen med frontend. \
Du trenger kun legge til de variablene som er aktuelle. De som mangler vil defaulte til mock.

**.env config som brukes av docker-compose**
```
API_URL=<TOM ELLER URL TIL LOKALT API>
BREV_API_URL=<TOM ELLER URL TIL LOKALT API>
VILKAARSVURDERING_API_URL=<TOM ELLER URL TIL LOKALT API>
```

#### Oppsett av frontend

Kjøres fra mappen `client`. Prosjektet bruker [Vite](https://vitejs.dev/).

1. Kjør `docker-compose up -d` \
    Dette starter wonderwall, mock-oauth2-server, og frackend i docker.
2. Start frontend (client):
    `yarn dev`
3. Du når frontend *via* wonderwall på [localhost:3000](http://localhost:3000) \
    Frontend kjører nå mot autentisert frackend og har gyldig Authorization i header.

## Miljøvariabler for lokal kjøring
`BREV_DEV` kan settes til `true` hvis man vil gå mot en lokal instans av `etterlatte-brev-api`.
`VILKAARSVURDERING_DEV` kan settes til `true` hvis man vil gå mot en lokal instans av `etterlatte-vilkaarsvurdering`.


## Bygg og deploy
Bygg og deploy kjøres via github workflows i roten på prosjektet
og kjører bygg av både client og server folder. \
Se Dockerfile for oppsett av container-image