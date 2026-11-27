# Utvidet omstillingsstønad

Agenten `utvidet-oms`, dokumentasjonen og statusskriptet er flyttet til søknadsrepoet `pensjon-etterlatte` mens vi bygger søknaden.

- [Veikart og arbeidspakker](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/veikart.md)
- [Beslutninger](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/beslutninger.md)
- [Spesifikasjon](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/spesifikasjon.md)
- [Mottak via prosessering (fase 5)](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/prosessering.md): teknisk plan for tasks i `innsendt-soeknad`, med journalføringskontekst og idempotent overlevering via Gjennys API (B12)
- [Agent](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/agents/utvidet-oms.md)

Start `copilot` fra `pensjon-etterlatte`, og velg `utvidet-oms` med `/agent`. Legg til Gjenny med `/add-dir ../pensjon-etterlatte-saksbehandling` når du trenger kode eller domenekontekst herfra. Statusskriptet kjøres fra søknadsrepoet: `.github/scripts/utvidet-oms-status.sh [--fetch]`.

Fase 5 blir dokumentert i søknadsrepoet etter B12. Innsendt-soeknad eier mottak, PDF og journalføring. Gjenny beholder avgjørelsene om sak, oppgave og behandling. Det skal ikke opprettes en ny kopi av hele søknaden eller nye journalføringsklienter i behandling for dette mottaket.

Sb #9049 og felles #642 bygger på den erstattede planen. Utvikleren vil lukke dem; dokumentasjonsendringen lukker ingen PR-er og endrer ingen deployment. Dagens Kafka-mottak og eksisterende prosessering i behandling beholdes.

En eventuell senere flytting av dokumentasjonen ved arbeid på behandlingsløpet (B7) vurderes separat. Domenekonteksten og agenten `gjenny-ekspert` blir her hele tiden.
