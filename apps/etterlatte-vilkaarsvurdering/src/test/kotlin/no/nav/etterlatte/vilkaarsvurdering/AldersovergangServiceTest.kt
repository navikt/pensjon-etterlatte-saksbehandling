package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AldersovergangServiceTest {
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val aldersAldersovergangService = AldersovergangService(vilkaarsvurderingService)

    private val behandlingId = UUID.randomUUID()
    private val loependeBehandlingId = UUID.randomUUID()
    private val idVilkaarForOppdatering = UUID.randomUUID()
    private val brukerTokenInfo = mockk<BrukerTokenInfo>()

    @Test
    fun `aldersovergang skal orkestrere opprettelse av vilkaarsvurdering, avslag paa vilkar og totalresultat`() {
        val vilkaarsvurdering =
            Vilkaarsvurdering(
                behandlingId = behandlingId,
                grunnlagVersjon = 1,
                virkningstidspunkt = YearMonth.now(),
                vilkaar =
                    listOf(
                        Vilkaar(
                            id = idVilkaarForOppdatering,
                            hovedvilkaar =
                                Delvilkaar(
                                    type = VilkaarType.BP_ALDER_BARN_2024,
                                    tittel = "Aldersvilkår",
                                    lovreferanse = Lovreferanse(paragraf = "§dummy"),
                                ),
                        ),
                        Vilkaar(
                            id = UUID.randomUUID(),
                            hovedvilkaar =
                                Delvilkaar(
                                    type = VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                                    tittel = "Fortsatt medlemskap",
                                    lovreferanse = Lovreferanse(paragraf = "§dummy"),
                                ),
                        ),
                    ),
            )

        val tidspunkt = LocalDateTime.of(2024, Month.MARCH, 13, 10, 28)

        val ikkeOppfyltVilkaar =
            VurdertVilkaar(
                vilkaarId = idVilkaarForOppdatering,
                hovedvilkaar =
                    VilkaarTypeOgUtfall(
                        type = VilkaarType.BP_ALDER_BARN_2024,
                        resultat = Utfall.IKKE_OPPFYLT,
                    ),
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "Aldersgrensen er passert",
                        tidspunkt = tidspunkt,
                        saksbehandler = Fagsaksystem.EY.navn,
                    ),
            )

        val vilkaarsvurderingResultat =
            VilkaarsvurderingResultat(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
                kommentar = "Automatisk aldersovergang",
                tidspunkt = tidspunkt,
                saksbehandler = Fagsaksystem.EY.navn,
            )

        coEvery { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId) } returns null
        coEvery {
            vilkaarsvurderingService.kopierVilkaarsvurdering(
                behandlingId,
                loependeBehandlingId,
                brukerTokenInfo,
                false,
            )
        } returns VilkaarsvuderingMedBehandlingGrunnlagsversjon(vilkaarsvurdering, 1L)
        coEvery {
            vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(behandlingId, brukerTokenInfo, ikkeOppfyltVilkaar)
        } returns vilkaarsvurdering
        coEvery {
            vilkaarsvurderingService.oppdaterTotalVurdering(behandlingId, brukerTokenInfo, vilkaarsvurderingResultat)
        } returns vilkaarsvurdering

        runBlocking {
            val res =
                aldersAldersovergangService.behandleOpphoerAldersovergang(
                    behandlingId = behandlingId,
                    loependeBehandlingId = loependeBehandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                    tidspunkt = { tidspunkt },
                )

            res shouldBe vilkaarsvurdering
        }

        coVerify { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId) }
        coVerify {
            vilkaarsvurderingService.kopierVilkaarsvurdering(
                behandlingId,
                loependeBehandlingId,
                brukerTokenInfo,
                false,
            )
        }
        coVerify {
            vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                behandlingId,
                brukerTokenInfo,
                ikkeOppfyltVilkaar,
            )
        }
        coVerify {
            vilkaarsvurderingService.oppdaterTotalVurdering(
                behandlingId,
                brukerTokenInfo,
                vilkaarsvurderingResultat,
            )
        }
    }
}
