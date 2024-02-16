# etterlatte-tidshendelser

Denne appen styrer jobber som kjører med jevne mellomrom for å håndtere nettopp det at tiden går, og som dermed medfører at saker må behandles på nytt, mtp f.eks. opphør av barnepensjon når vedkommende når en viss alder.

## Jobber - scenarier

- Aldersovergang
  - Opphør barnepensjon ved fylte 20 år
    - Utvidet aldersgrense til 21 år dersom yrkesskadefordel før 01.01.2024 (Reformtidspunkt)
  - Opphør barnepensjon ved fylte 21 år

## Oversikt

```mermaid
graph 
    
subgraph tidshendelser-app
  db[(database)]
  jobbPoller[Poller jobber \n som skal kjøres]
  service[Behandle jobb]
  hendelsePoller[Poller for \n nye hendelser]
  status[Oppdatere status, data]
  
  db --> jobbPoller
  jobbPoller --> service
  service --lagre hendelser--> db
  db --> hendelsePoller
  status --> db
end
  
kt{{kafka-etterlatte}}:::kafka-topic

subgraph grunnlag
    grunnlag_sjekk["Saker hvor bruker \n fyller n år"]
end
subgraph vedtaksvurdering
    vedtak_sjekk["Sjekke løpende ytelse"]
end
subgraph vilkaarsvurdering
    vilkaar_sjekk["Sjekke yrkesskadefordel \n pr reformtidspunkt"]
end
subgraph behandling
    behandle["Opprette oppgave"]
end

service ---> grunnlag["Grunnlag API"]
hendelsePoller ==publiser=====> kt
kt --> status
kt <--> vedtaksvurdering
kt <--> vilkaarsvurdering
kt <--> behandling
```

### Teknologi
kotlin/ktor, postgres, kakfa (rapids & rivers)

## Lokal utvikling

Les [README](../../README.md) på rot i prosjektet.

