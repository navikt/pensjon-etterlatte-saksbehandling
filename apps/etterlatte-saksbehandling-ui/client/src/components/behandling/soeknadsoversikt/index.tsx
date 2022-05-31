import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold, FamilieforholdWrapper } from './familieforhold/Familieforhold'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
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
  const gyldigFramsatt = ctx.state.behandlingReducer.gyldighetsprøving
  const kommerSoekerTilgode = ctx.state.behandlingReducer.kommerSoekerTilgode

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

      <SoeknadOversikt gyldigFramsatt={gyldigFramsatt} kommerSoekerTilgode={kommerSoekerTilgode} />
      <Border />
      {gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT ? (
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
        <Start soeknadGyldigFremsatt={gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT} />
      </BehandlingHandlingKnapper>
    </Content>
  )
}
