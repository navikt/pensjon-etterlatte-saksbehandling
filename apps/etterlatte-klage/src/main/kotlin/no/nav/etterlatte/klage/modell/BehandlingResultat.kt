package no.nav.etterlatte.klage.modell

enum class Aarsak {
    FEIL_I_LOVANDVENDELSE,
    FEIL_REGELVERKSFORSTÅELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    FEIL_PROSESSUELL,
    ANNET,
}

enum class BehandlingEventType {
    KLAGEBEHANDLING_AVSLUTTET,
    ANKEBEHANDLING_OPPRETTET,
    ANKEBEHANDLING_AVSLUTTET,
    ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
    BEHANDLING_FEILREGISTRERT,
// TODO ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET skal fjernes på sikt
}

enum class KlageinstansUtfall(val navn: String) {
    TRUKKET("Trukket KA"),
    RETUR("Retur KA"),
    OPPHEVET("Opphevet KA"),
    MEDHOLD("Medhold KA"),
    DELVIS_MEDHOLD("Delvis medhold KA"),
    STADFESTELSE("Stadfestelse KA"),
    UGUNST("Ugunst (Ugyldig) KA"),
    AVVIST("Avvist KA"),
    INNSTILLING_STADFESTELSE("Innstilling om stadfestelse til trygderetten fra KA"),
    INNSTILLING_AVVIST("Innstilling om avist til trygderetten fra KA"),
}
