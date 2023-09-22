package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth

class TilbakekrevingService(
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentTilbakekreving(tilbakerevingId: String): Tilbakekreving {
        return Tilbakekreving(
            id = 123L,
            status = TilbakekrevingStatus.OPPRETTET,
            sak = Sak(id = 474L, ident = "10078201296", sakType = SakType.OMSTILLINGSSTOENAD, enhet = "4862"),
            opprettet = Tidspunkt.now(),
            kravgrunnlag =
                Kravgrunnlag(
                    kravgrunnlagId = KravgrunnlagId(123L),
                    sakId = SakId(474L),
                    vedtakId = VedtakId(2L),
                    kontrollFelt = Kontrollfelt(""),
                    status = KravgrunnlagStatus.ANNU,
                    saksbehandler = NavIdent(""),
                    sisteUtbetalingslinjeId = UUID30(""),
                    perioder =
                        listOf(
                            KravgrunnlagPeriode(
                                periode =
                                    Periode(
                                        fraOgMed = YearMonth.of(2023, 1),
                                        tilOgMed = YearMonth.of(2023, 2),
                                    ),
                                skatt = BigDecimal(200),
                                grunnlagsbeloep =
                                    listOf(
                                        Grunnlagsbeloep(
                                            kode = KlasseKode(""),
                                            type = KlasseType.YTEL,
                                            bruttoUtbetaling = BigDecimal(1000),
                                            beregnetNyBrutto = BigDecimal(1200),
                                            bruttoTilbakekreving = BigDecimal(200),
                                            nettoTilbakekreving = BigDecimal(200),
                                            beloepSkalIkkeTilbakekreves = BigDecimal(200),
                                            skatteProsent = BigDecimal(20),
                                            resultat = null,
                                            skyld = null,
                                            aarsak = null,
                                        ),
                                        Grunnlagsbeloep(
                                            kode = KlasseKode(""),
                                            type = KlasseType.FEIL,
                                            bruttoUtbetaling = BigDecimal(0),
                                            beregnetNyBrutto = BigDecimal(0),
                                            bruttoTilbakekreving = BigDecimal(0),
                                            nettoTilbakekreving = BigDecimal(0),
                                            beloepSkalIkkeTilbakekreves = BigDecimal(0),
                                            skatteProsent = BigDecimal(0),
                                            resultat = null,
                                            skyld = null,
                                            aarsak = null,
                                        ),
                                    ),
                            ),
                        ),
                ),
        )
    }

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) =
        inTransaction {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = kravgrunnlag.kravgrunnlagId.value.toString(),
                sakId = kravgrunnlag.sakId.value,
                oppgaveKilde = OppgaveKilde.EKSTERN,
                oppgaveType = OppgaveType.TILBAKEKREVING,
                merknad = null,
            )
        }
}
