package no.nav.etterlatte.libs.common.person

// behandlingsnummere: https://behandlingskatalog.nais.adeo.no/team/cf8730ca-3aa7-4a94-97ec-75bcc620b63f
enum class Behandlingsnummer(val behandlingsnummer: String) {
    BARNEPENSJON("B359"),
    OMSTILLINGSSTOENAD("B373"),
}
