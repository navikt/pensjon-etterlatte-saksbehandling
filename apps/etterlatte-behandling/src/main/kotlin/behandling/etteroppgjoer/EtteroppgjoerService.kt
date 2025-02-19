package no.nav.etterlatte.behandling.etteroppgjoer

import java.util.UUID

class EtteroppgjoerService(
    private val dao: EtteroppgjoerDao,
) {
    fun hentEtteroppgjoer(behandlingId: UUID): Etteroppgjoer {
        val etteroppgjoerBehandling = dao.hentEtteroppgjoer(behandlingId) ?: throw Exception("todo")
        return Etteroppgjoer(
            behandling = etteroppgjoerBehandling,
            // TODO egen tabell? I beregning?
            opplysninger =
                EtteroppgjoerOpplysninger(
                    skatt =
                        OpplysnignerSkatt(
                            aarsinntekt = 200000,
                        ),
                    ainntekt =
                        AInntekt(
                            inntektsmaaneder =
                                listOf(
                                    AInntektMaaned(
                                        maaned = "Januar",
                                        summertBeloep = 150000,
                                    ),
                                    AInntektMaaned(
                                        maaned = "Januar",
                                        summertBeloep = 150000,
                                    ),
                                ),
                        ),
                ),
        )
    }

    fun opprettEtteroppgjoer() {
        // dao.lagreEtteroppgjoer()
    }
}
