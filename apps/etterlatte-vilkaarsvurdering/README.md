# etterlatte-vilkaarsvurdering

Tjeneste som tilbyr endepunkter for å hente ut vilkår for en behandling og sette dem til oppfylt / ikke oppfylt.

Det som støttes så langt er
- Hovedvilkår
- Unntaksvilkår

Utestående funksjonalitet er
- Grunnlag på vilkår
- Automatiserte vilkårsvurdering

## Kjøre lokalt (auth fra dev-gcp + db lokalt)
1. For å sette opp riktig konfigurasjon for applikasjonen, kjør scriptet `get-secret.sh` fra prosjektets [rotmappe](../..).
```
./get-secret.sh etterlatte-vilkaarsvurdering
```
2. Kjør opp en lokal postgres-database:
```
docker-compose up -d
```
3. Om du skal kjøre med frontend og wonderwall må du også kjøre (fra rotmappe):
```
./get-secret.sh etterlatte-saksbehandling-ui
```
og legge til følgende linjer nederst i `.env.dev-gcp` fila til saksbehandling-ui.
```
VILKAARSVURDERING_API_URL=http://host.docker.internal:8087
VILKAARSVURDERING_API_SCOPE=api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default
```
4. Start applikasjonen