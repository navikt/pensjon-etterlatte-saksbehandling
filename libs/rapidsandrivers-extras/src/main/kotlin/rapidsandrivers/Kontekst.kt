package no.nav.etterlatte.rapidsandrivers

enum class Kontekst(
    val retries: Int,
) {
    BREV(1),
    DOEDSHENDELSE(2),
    MIGRERING(1),
    REGULERING(1),
    VENT(0),
    TEST(0),
}
