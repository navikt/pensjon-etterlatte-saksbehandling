package no.nav.etterlatte.rapidsandrivers

enum class Kontekst(
    val retries: Int,
) {
    BREV(1),
    DOEDSHENDELSE(2),
    MIGRERING(1),
    REGULERING(0),
    AARLIG_INNTEKTSJUSTERING(0),
    OMREGNING(0),
    INNTEKTSJUSTERING(0),
    VENT(0),
    TEST(0),
}
