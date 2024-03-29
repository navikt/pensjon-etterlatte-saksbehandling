# Etterlatte-saksbehandling-ui

Appen består av en React frontend skrevet i Typescript og en backend-for-frontend med Express.

Når appen kjøres opp lokalt settes det også opp en mock-router som server statiske JSON for forskjellige behandlinger 
for testing.

---

## Kjøring lokalt

Avhengigheter må installeres for at prosjektet skal kjøre. Dette kan du gjøre ved å kjøre kommandoen `yarn` i enten 
ui sin rot, eller i [client](./client) og [server](./server) individuelt.

Kjøres fra mappen `client` og/eller `server`. \
Prosjektet bruker [Vite](https://vitejs.dev/).

### Sett opp kobling mot APIer i dev-gcp

#### Docker host

For å kjøre mot backend lokalt med docker må det være mulig å nå `host.docker.internal`, så pass på at du legger til 
dette i `/etc/hosts`:

`127.0.0.1 host.docker.internal`


#### Uthenting av secrets

For å kjøre mot dev-gcp må du først kjøre `get-secret.sh` ([../../get-secret.sh](../../get-secret.sh)). \
Felles bruksanvisning til scriptet finner du på [prosjektets rot](../..). 

Scriptet henter ut secrets for lokal kjøring mot dev-gcp og lagrer de i en lokal fil (`.env.dev-gcp`). 
Denne filen skal inneholde en rekke miljøvariabler som brukes av frackend og wonderwall. 

Om du ønsker å gå mot en backend-app lokalt kan du legge til miljøvariabler for dette i `.env.dev-gcp`. \
Se i filen [config.ts](./server/src/config/config.ts) for å se gyldige nøkler. Trenger kun legge til de 
du skal benytte deg av lokalt. Resten vil automatisk gå mot dev.

Når `.env.dev-gcp` er opprettet kan du kjøre opp docker (dev-gcp varianten):

1. Kjør `docker-compose up -d` \
   Dette starter wonderwall og frackend i docker, _med_ gyldig AzureAD config.
2. Start frontend (client):
   `yarn dev`
3. Du når frontend *via* wonderwall på [localhost:3000](http://localhost:3000) \
   Frontend kjører nå mot autentisert frackend og har gyldig Authorization i header.

**OBS:** Hvis du gjør endringer i frackend kan det hende du må bygge den på nytt. Kan gjøres ved å legge til
`--build --force-recreate` i kommandoen over.


### Azuread lokal

For å få tilgang til dev-gcp lokalt bruker vi egne secrets for frontend. Filen sikrer at det gis tilgang til appene 
frontend kobler seg mot i dev-gcp. Filen finner du under `.nais/`

Dersom filen endres kan denne kommandoen kjøres (OBS: Dette medfører at alle må hente secrets på nytt). 

```
kubectl apply -f .nais/azuread-etterlatte-saksbehandling-ui-lokal.yaml
```

Det er kun frontend som trenger denne filen. Ved kjøring av backend-apper er det bedre å kjøre frontend mot lokal
backend og kjøre opp den lokale backenden med secrets fra dev-gcp. Bare kjør `get-secret.sh` for ønsket backend-app,
så vil det genereres secrets for den appen.

---

## Bygg og deploy
Bygg og deploy kjøres via github workflows i roten på prosjektet
og kjører bygg av både client og server folder. \
Se Dockerfile for oppsett av container-image