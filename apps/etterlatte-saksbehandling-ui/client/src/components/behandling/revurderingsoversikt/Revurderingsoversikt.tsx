import { Content, ContentHeader } from '~shared/styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Familieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/Familieforhold'
import { Innhold, Border } from '~components/behandling/soeknadsoversikt/styled'
import { HeadingWrapper } from '~components/person/saksoversikt'

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Revurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <Innhold>
        <Virkningstidspunkt
          redigerbar={behandles}
          virkningstidspunkt={behandling.virkningstidspunkt}
          avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
          avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
          soeknadMottattDato={behandling.soeknadMottattDato}
          behandlingId={behandling.id}
          sakstype={behandling.sakType}
        />
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
