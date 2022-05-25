import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold, FamilieforholdWrapper } from './familieforhold/Familieforhold'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'
import { GyldigFramsattType, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { Border, HeadingWrapper } from './styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Start } from '../handlinger/start'
import { Soeknadsdato } from './soeknadoversikt/soeknadinfo/Soeknadsdato'
import { GjenlevendeForelder } from './familieforhold/personer/GjenlevendeForelder'

export const Soeknadsoversikt = () => {
  const {
    soekerPdl,
    avdoedPersonPdl,
    soekerSoknad,
    avdodPersonSoknad,
    innsender,
    mottattDato,
    gjenlevendePdl,
    gjenlevendeSoknad,
  } = usePersonInfoFromBehandling()

  const ctx = useContext(AppContext)
  const gyldighet = ctx.state.behandlingReducer.gyldighetsprøving

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="xlarge" level="5">
            Søknadsoversikt
          </Heading>
          <div className="details">
            <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
            <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
          </div>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={mottattDato} />
      </ContentHeader>

      <SoeknadOversikt
        gyldighet={gyldighet}
        avdoedPersonPdl={avdoedPersonPdl}
        mottattDato={mottattDato}
        innsenderHarForeldreansvar={gyldighet.vurderinger.find(
          (g) => g.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET
        )}
        gjenlevendeOgSoekerLikAdresse={gyldighet.vurderinger.find(
          (g) => g.navn === GyldigFramsattType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL
        )}
        innsenderErForelder={gyldighet.vurderinger.find((g) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER)}
      />
      <Border />
      {gyldighet.resultat === VurderingsResultat.OPPFYLT ? (
        <Familieforhold
          soekerPdl={soekerPdl}
          soekerSoknad={soekerSoknad}
          avdoedPersonPdl={avdoedPersonPdl}
          avdodPersonSoknad={avdodPersonSoknad}
          gjenlevendePdl={gjenlevendePdl}
          gjenlevendeSoknad={gjenlevendeSoknad}
          innsender={innsender}
        />
      ) : (
        innsender.foedselsnummer === gjenlevendePdl.foedselsnummer && (
          <>
            <FamilieforholdWrapper>
              <GjenlevendeForelder
                person={{
                  navn: `${gjenlevendePdl?.fornavn} ${gjenlevendePdl?.etternavn}`,
                  fnr: gjenlevendePdl?.foedselsnummer,
                  statsborgerskap: gjenlevendePdl?.statsborgerskap,
                  adresser: gjenlevendePdl?.bostedsadresse,
                  fnrFraSoeknad: gjenlevendeSoknad?.foedselsnummer,
                  adresseFraSoeknad: gjenlevendeSoknad?.adresse,
                }}
                innsenderErGjenlevende={innsender.foedselsnummer === gjenlevendePdl.foedselsnummer}
              />
            </FamilieforholdWrapper>
            <Border />
          </>
        )
      )}

      <BehandlingHandlingKnapper>
        <Start soeknadGyldigFremsatt={gyldighet.resultat === VurderingsResultat.OPPFYLT} />
      </BehandlingHandlingKnapper>
    </Content>
  )
}
