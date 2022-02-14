# post-til-kafka
___
[Naisjob](https://doc.nais.io/naisjob/) for å poste en melding (søknad) til kafka. Benyttes ifm. integrasjonstesting av søknadsflyten.


For å trigge denne jobben gjøres følgende:
1. Finn docker image versjon som skal benyttes av denne jobben.
   1. Gå til siste bygg i [GitHub actions](https://github.com/navikt/pensjon-etterlatte/actions/workflows/job-post-til-kafka.yaml) og sjekk loggene for image versjon.
   2. Oppdater deploy-fila (`dev.yml` eller `prod.yml`) tilhørende miljøet som skal kjøre jobben under `deploy` med riktig versjon.
2. Sjekk at det ikke henger igjen noe fra forrige kjøring ved å slette informasjon om eventuelle tidligere jobber:
   1. ```
      kubectl delete naisjob post-til-kafka
      kubectl delete job post-til-kafka
      ```
3. Start jobben med `kubectl apply -f deploy/(dev|prod).yml`
4. Etter endt kjøring bør jobben slettes så det er klart til neste gang, se steg 2.