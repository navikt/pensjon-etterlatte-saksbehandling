import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold } from './familieforhold/Familieforhold'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { Border, HeadingWrapper } from './styled'
import { BehandlingsType } from '../behandlingsType'
import { ISaksType, SaksType } from '../saksType'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Start } from '../handlinger/start'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '../felles/utils'

export const Soeknadsoversikt = () => {
  const behandling = useContext(AppContext).state.behandlingReducer
  const behandles = hentBehandlesFraStatus(behandling?.status)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="xlarge" level="5">
            Søknadsoversikt
          </Heading>
          <div className="details">
            <BehandlingsType type={behandling.behandlingType} />
            <SaksType type={ISaksType.BARNEPENSJON} />
          </div>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />
      </ContentHeader>
      <SoeknadOversikt behandling={behandling} />
      <Border />
      <Familieforhold behandling={behandling} />
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Start soeknadGyldigFremsatt={behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT} />
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
