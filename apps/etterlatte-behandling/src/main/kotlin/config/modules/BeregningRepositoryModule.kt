package no.nav.etterlatte.config.modules

import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.modules.beregning.AnvendtTrygdetidRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.AvkortingRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.BeregningRepository
import no.nav.etterlatte.modules.beregning.BeregningRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.BeregningsGrunnlagRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.EtteroppgjoerRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.SanksjonRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidRepository
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerRepository
import no.nav.etterlatte.modules.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.modules.beregning.klienter.BeregningCrudKlient
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonRepository
import javax.sql.DataSource

class BeregningRepositoryModule(
    dataSource: DataSource,
    env: Miljoevariabler,
    crudKlient: () -> BeregningCrudKlient,
) {
    private val brukLokalDatabase =
        env.getOrDefault(BeregningDatabaseKey.BRUK_EGEN_DATABASE_FOR_BEREGNING, "false").toBooleanStrict()
    private val eksternDatabase by lazy(crudKlient)

    val beregningRepository: BeregningRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) BeregningRepository(dataSource) else eksternDatabase
    }

    val beregningsGrunnlagRepository: BeregningsGrunnlagRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) BeregningsGrunnlagRepository(dataSource) else eksternDatabase
    }

    val anvendtTrygdetidRepository: AnvendtTrygdetidRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) AnvendtTrygdetidRepository(dataSource) else eksternDatabase
    }

    val avkortingRepository: AvkortingRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) AvkortingRepository(dataSource) else eksternDatabase
    }

    val etteroppgjoerRepository: EtteroppgjoerRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) EtteroppgjoerRepository(dataSource) else eksternDatabase
    }

    val sanksjonRepository: SanksjonRepositoryOperasjoner by lazy {
        if (brukLokalDatabase) SanksjonRepository(dataSource) else eksternDatabase
    }
}

enum class BeregningDatabaseKey : EnvEnum {
    BRUK_EGEN_DATABASE_FOR_BEREGNING,
    ;

    override fun key() = name
}
