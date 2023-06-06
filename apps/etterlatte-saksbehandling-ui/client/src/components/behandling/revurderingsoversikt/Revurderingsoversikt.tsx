import { Content, ContentHeader } from '~shared/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '../felles/utils'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Border, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { HeadingWrapper } from '~components/person/saksoversikt'
import {
  BP_OPPHOER_BESKRIVELSE,
  BP_OPPHOER_HJEMLER,
  BP_REVURDERING_BESKRIVELSE,
  BP_REVURDERING_HJEMLER,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'
import { erOpphoer, Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import styled from 'styled-components'

const revurderingsaarsakTilTekst = (revurderingsaarsak: Revurderingsaarsak | null | undefined): string =>
  !revurderingsaarsak ? 'ukjent årsak' : tekstRevurderingsaarsak[revurderingsaarsak]

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const opphoer = behandling?.revurderingsaarsak && erOpphoer(behandling?.revurderingsaarsak)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Revurdering
          </Heading>
        </HeadingWrapper>
        <BodyShort>
          Revurdering på grunn av <Lowercase>{revurderingsaarsakTilTekst(behandling.revurderingsaarsak)}</Lowercase>.
        </BodyShort>
      </ContentHeader>
      <Innhold>
        <Virkningstidspunkt
          redigerbar={behandles}
          virkningstidspunkt={behandling.virkningstidspunkt}
          avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
          avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
          soeknadMottattDato={behandling.soeknadMottattDato}
          behandlingId={behandling.id}
          hjemmler={opphoer ? BP_OPPHOER_HJEMLER : BP_REVURDERING_HJEMLER}
          beskrivelse={opphoer ? BP_OPPHOER_BESKRIVELSE : BP_REVURDERING_BESKRIVELSE}
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

const Lowercase = styled.span`
  text-transform: lowercase;
`
