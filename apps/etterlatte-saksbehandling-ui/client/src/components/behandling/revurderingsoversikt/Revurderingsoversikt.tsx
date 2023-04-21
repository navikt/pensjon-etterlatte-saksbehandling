import { Content, ContentHeader } from '~shared/styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '../felles/utils'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Innhold, Border } from '~components/behandling/soeknadsoversikt/styled'
import { HeadingWrapper } from '~components/person/saksoversikt'
import {
  BP_REVURDERING_BESKRIVELSE,
  BP_REVURDERING_HJEMLER,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'

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
          hjemmler={BP_REVURDERING_HJEMLER}
          beskrivelse={BP_REVURDERING_BESKRIVELSE}
        />
      </Innhold>
      <Border />
      {behandles ? (
        <BehandlingHandlingKnapper>
          {<Start disabled={behandling.virkningstidspunkt === null} />}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
