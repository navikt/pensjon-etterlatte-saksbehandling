import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold } from './familieforhold/Familieforhold'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'
import { GyldighetType, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { HeadingWrapper } from './styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Start } from '../handlinger/start'

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
      </ContentHeader>

      <SoeknadOversikt
        gyldighet={gyldighet}
        avdoedPersonPdl={avdoedPersonPdl}
        innsender={innsender}
        mottattDato={mottattDato}
        gjenlevendePdl={gjenlevendePdl}
        gjenlevendeHarForeldreansvar={gyldighet.vurderinger.find(
          (g) => g.navn === GyldighetType.HAR_FORELDREANSVAR_FOR_BARNET
        )}
        gjenlevendeOgSoekerLikAdresse={gyldighet.vurderinger.find(
          (g) => g.navn === GyldighetType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL
        )}
        innsenderHarForeldreAnsvar={gyldighet.vurderinger.find((g) => g.navn === GyldighetType.INNSENDER_ER_FORELDER)}
      />
      <Familieforhold
        soekerPdl={soekerPdl}
        soekerSoknad={soekerSoknad}
        avdoedPersonPdl={avdoedPersonPdl}
        avdodPersonSoknad={avdodPersonSoknad}
        gjenlevendePdl={gjenlevendePdl}
        gjenlevendeSoknad={gjenlevendeSoknad}
        innsender={innsender}
      />
      <BehandlingHandlingKnapper>
        <Start soeknadGyldigFremsatt={gyldighet.resultat === VurderingsResultat.OPPFYLT} />
      </BehandlingHandlingKnapper>
    </Content>
  )
}
