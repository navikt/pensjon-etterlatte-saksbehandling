import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, Innhold } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/soeknadoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Søknadsoversikt
          </Heading>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />
      </ContentHeader>
      <Innhold>
        <OversiktGyldigFramsatt behandling={behandling} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == ISaksType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={behandles}
                søker={behandling.søker}
                forelder={behandling.familieforhold?.gjenlevende}
                behandlingId={behandling.id}
              />
            )}
            <Virkningstidspunkt
              redigerbar={behandles}
              virkningstidspunkt={behandling.virkningstidspunkt}
              avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
              avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
              soeknadMottattDato={behandling.soeknadMottattDato}
              behandlingId={behandling.id}
              sakstype={behandling.sakType}
            />
          </>
        )}
      </Innhold>
      <Border />
      <Familieforhold behandling={behandling} />
      {behandles ? (
        <BehandlingHandlingKnapper>
          {behandlingErUtfylt(behandling) && (
            <Start soeknadGyldigFremsatt={behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT} />
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
