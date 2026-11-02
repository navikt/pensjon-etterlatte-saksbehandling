# Utvidet omstillingsstønad

Agenten `utvidet-oms`, dokumentasjonen og statusskriptet er flyttet til søknadsrepoet `pensjon-etterlatte` mens vi bygger søknaden.

- [Veikart og arbeidspakker](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/veikart.md)
- [Beslutninger](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/beslutninger.md)
- [Spesifikasjon](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/utvidet-oms/spesifikasjon.md)
- [Agent](https://github.com/navikt/pensjon-etterlatte/blob/main/.github/agents/utvidet-oms.md)

Start `copilot` fra `pensjon-etterlatte`, og velg `utvidet-oms` med `/agent`. Legg til Gjenny med `/add-dir ../pensjon-etterlatte-saksbehandling` når du trenger kode eller domenekontekst herfra. Statusskriptet kjøres fra søknadsrepoet: `.github/scripts/utvidet-oms-status.sh [--fetch]`.

Når arbeidet går over til behandlingsløpet i Gjenny, flyttes agenten, dokumentasjonen og statusskriptet samlet tilbake hit. Oppdater hjemrepo, stier og lenker samtidig (B7). Domenekonteksten og agenten `gjenny-ekspert` blir her hele tiden.
