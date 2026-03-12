package no.nav.etterlatte.config.modules

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktBrevDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserDao
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingDao
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJobDao
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevDao
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.UkjentBeroertDao
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.metrics.BehandlingMetrikkerDao
import no.nav.etterlatte.metrics.GjenopprettingMetrikkerDao
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.kommentar.OppgaveKommentarDaoImpl
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.vilkaarsvurdering.dao.DelvilkaarDao
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingDao
import javax.sql.DataSource

class DaoModule(
    private val autoClosingDatabase: ConnectionAutoclosing,
    private val dataSource: DataSource,
) {
    val hendelseDao by lazy { HendelseDao(autoClosingDatabase) }
    val kommerBarnetTilGodeDao by lazy { KommerBarnetTilGodeDao(autoClosingDatabase) }
    val aktivitetspliktDao by lazy { AktivitetspliktDao(autoClosingDatabase) }
    val aktivitetspliktAktivitetsgradDao by lazy { AktivitetspliktAktivitetsgradDao(autoClosingDatabase) }
    val aktivitetspliktUnntakDao by lazy { AktivitetspliktUnntakDao(autoClosingDatabase) }
    val sjekklisteDao by lazy { SjekklisteDao(autoClosingDatabase) }
    val revurderingDao by lazy { RevurderingDao(autoClosingDatabase) }

    val behandlingDao by lazy {
        BehandlingDao(
            kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
            revurderingDao = revurderingDao,
            connectionAutoclosing = autoClosingDatabase,
        )
    }

    val generellbehandlingDao by lazy { GenerellBehandlingDao(autoClosingDatabase) }
    val behandlingMedBrevDao by lazy { BehandlingMedBrevDao(autoClosingDatabase) }

    private val oppgaveDaoNy by lazy { OppgaveDaoImpl(autoClosingDatabase) }
    val oppgaveDaoEndringer by lazy { OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy, autoClosingDatabase) }
    val oppgaveKommentarDao by lazy { OppgaveKommentarDaoImpl(autoClosingDatabase) }

    val sakLesDao by lazy { SakLesDao(autoClosingDatabase) }
    val sakendringerDao by lazy { SakendringerDao(autoClosingDatabase) }
    val sakSkrivDao by lazy { SakSkrivDao(sakendringerDao) }
    val sakTilgangDao by lazy { SakTilgangDao(dataSource) }

    val grunnlagsendringshendelseDao by lazy { GrunnlagsendringshendelseDao(autoClosingDatabase) }
    val institusjonsoppholdDao by lazy { InstitusjonsoppholdDao(autoClosingDatabase) }

    val oppgaveMetrikkerDao by lazy { OppgaveMetrikkerDao(dataSource) }
    val behandlingMetrikkerDao by lazy { BehandlingMetrikkerDao(dataSource) }
    val gjenopprettingMetrikkerDao by lazy { GjenopprettingMetrikkerDao(dataSource) }

    val klageDao by lazy { KlageDaoImpl(autoClosingDatabase) }
    val tilbakekrevingDao by lazy { TilbakekrevingDao(autoClosingDatabase) }

    val etteroppgjoerForbehandlingDao by lazy { EtteroppgjoerForbehandlingDao(autoClosingDatabase) }
    val skatteoppgjoerHendelserDao by lazy { SkatteoppgjoerHendelserDao(autoClosingDatabase) }
    val etteroppgjoerDao by lazy { EtteroppgjoerDao(autoClosingDatabase) }

    val behandlingInfoDao by lazy { BehandlingInfoDao(autoClosingDatabase) }
    val bosattUtlandDao by lazy { BosattUtlandDao(autoClosingDatabase) }
    val saksbehandlerInfoDao by lazy { SaksbehandlerInfoDao(autoClosingDatabase) }
    val aktivitetspliktBrevDao by lazy { AktivitetspliktBrevDao(autoClosingDatabase) }
    val doedshendelseDao by lazy { DoedshendelseDao(autoClosingDatabase) }
    val omregningDao by lazy { OmregningDao(autoClosingDatabase) }

    val vilkaarsvurderingDao by lazy { VilkaarsvurderingDao(autoClosingDatabase, DelvilkaarDao()) }
    val ukjentBeroertDao by lazy { UkjentBeroertDao(autoClosingDatabase) }

    val aldersovergangDao by lazy { AldersovergangDao(dataSource) }
    val opplysningDao by lazy { OpplysningDao(dataSource) }

    val sjekkAdressebeskyttelseJobDao by lazy { SjekkAdressebeskyttelseJobDao(autoClosingDatabase) }
}
