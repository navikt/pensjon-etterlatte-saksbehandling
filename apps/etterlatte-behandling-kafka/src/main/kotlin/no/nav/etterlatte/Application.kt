package no.nav.etterlatte

import no.nav.etterlatte.brev.OppdaterDoedshendelseBrevDistribuert
import no.nav.etterlatte.brukerdialog.inntektsjustering.InntektsjusteringRiver
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.OmsMeldtInnEndringRiver
import no.nav.etterlatte.brukerdialog.soeknad.NySoeknadRiver
import no.nav.etterlatte.brukerdialog.soeknad.OpprettBehandlingRiver
import no.nav.etterlatte.grunnlag.GrunnlagHendelserRiver
import no.nav.etterlatte.grunnlag.GrunnlagsversjoneringRiver
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringJobbRiver
import no.nav.etterlatte.migrering.AvbrytBehandlingHvisMigreringFeilaRiver
import no.nav.etterlatte.opplysningerfrasoknad.StartUthentingFraSoeknadRiver
import no.nav.etterlatte.opplysningerfrasoknad.uthenter.Opplysningsuthenter
import no.nav.etterlatte.pdl.PdlHendelserRiver
import no.nav.etterlatte.regulering.FinnSakerTilReguleringRiver
import no.nav.etterlatte.regulering.OmregningBrevDistribusjonRiver
import no.nav.etterlatte.regulering.OmregningFeiletRiver
import no.nav.etterlatte.regulering.OmregningsHendelserBehandlingRiver
import no.nav.etterlatte.regulering.ReguleringsforespoerselRiver
import no.nav.etterlatte.regulering.VedtakAttestertRiver
import no.nav.etterlatte.regulering.YtelseIkkeLoependeRiver
import no.nav.etterlatte.tidshendelser.TidshendelseRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR

fun main() {
    initRogR("oppdater-behandling") { rapidsConnection, rapidEnv ->
        settOppRivers(
            rapidsConnection,
            AppBuilder(rapidEnv),
        )
    }
}

private fun settOppRivers(
    rapidsConnection: RapidsConnection,
    appBuilder: AppBuilder,
) {
    val behandlingservice = appBuilder.behandlingService
    val tidshendelseService = appBuilder.tidshendelserService
    val featureToggleService = appBuilder.featureToggleService
    val vilkaarsvurderingService = appBuilder.vilkaarsvurderingService

    PdlHendelserRiver(rapidsConnection, behandlingservice)
    OmregningsHendelserBehandlingRiver(rapidsConnection, behandlingservice)
    FinnSakerTilReguleringRiver(rapidsConnection, behandlingservice)
    ReguleringsforespoerselRiver(rapidsConnection, behandlingservice, featureToggleService)
    OmregningFeiletRiver(rapidsConnection, behandlingservice)
    VedtakAttestertRiver(rapidsConnection, behandlingservice, featureToggleService)
    AvbrytBehandlingHvisMigreringFeilaRiver(rapidsConnection, behandlingservice)
    YtelseIkkeLoependeRiver(rapidsConnection, behandlingservice)
    TidshendelseRiver(rapidsConnection, tidshendelseService)
    OppdaterDoedshendelseBrevDistribuert(rapidsConnection, behandlingservice)
    AarligInntektsjusteringJobbRiver(rapidsConnection, behandlingservice, featureToggleService)
    OmregningBrevDistribusjonRiver(rapidsConnection, behandlingservice)

    StartUthentingFraSoeknadRiver(rapidsConnection, Opplysningsuthenter())

    // TODO tilgjengeliggjør disse riverene når etterlatte-vilkaarsvurdering-kafka er skrudd av
    // VilkaarsvurderingRiver(rapidsConnection, vilkaarsvurderingService)
    // TidshendelseVilkaarsvurderingRiver(rapidsConnection, vilkaarsvurderingService)

    NySoeknadRiver(
        rapidsConnection = rapidsConnection,
        behandlingKlient = appBuilder.behandlingKlient,
        journalfoerSoeknadService = appBuilder.journalfoerSoeknadService,
    )

    OpprettBehandlingRiver(
        rapidsConnection,
        appBuilder.behandlingKlient,
    )

    InntektsjusteringRiver(
        rapidsConnection,
        behandlingKlient = appBuilder.behandlingKlient,
        journalfoerInntektsjusteringService = appBuilder.journalfoerInntektsjusteringService,
    )

    OmsMeldtInnEndringRiver(
        rapidsConnection,
        behandlingKlient = appBuilder.behandlingKlient,
        journalfoerService = appBuilder.journalfoerOmsMeldtInnEndringService,
    )

    GrunnlagHendelserRiver(rapidsConnection, appBuilder.grunnlagKlient)
    GrunnlagsversjoneringRiver(rapidsConnection, appBuilder.grunnlagKlient)
}
