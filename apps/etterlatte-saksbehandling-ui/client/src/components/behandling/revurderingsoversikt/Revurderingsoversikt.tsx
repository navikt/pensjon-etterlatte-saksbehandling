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
  OMS_OPPHOER_BESKRIVELSE,
  OMS_OPPHOER_HJEMLER,
  OMS_REVURDERING_BESKRIVELSE,
  OMS_REVURDERING_HJEMLER,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { erOpphoer, Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import styled from 'styled-components'

const revurderingsaarsakTilTekst = (revurderingsaarsak: Revurderingsaarsak | null | undefined): string =>
  !revurderingsaarsak ? 'ukjent årsak' : tekstRevurderingsaarsak[revurderingsaarsak]

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const opphoer = behandling?.revurderingsaarsak && erOpphoer(behandling.revurderingsaarsak)

  const hjemmler = {
    [ISaksType.BARNEPENSJON]: opphoer ? BP_OPPHOER_HJEMLER : BP_REVURDERING_HJEMLER,
    [ISaksType.OMSTILLINGSSTOENAD]: opphoer ? OMS_OPPHOER_HJEMLER : OMS_REVURDERING_HJEMLER,
  }[behandling.sakType]

  const beskrivelse = {
    [ISaksType.BARNEPENSJON]: opphoer ? BP_OPPHOER_BESKRIVELSE : BP_REVURDERING_BESKRIVELSE,
    [ISaksType.OMSTILLINGSSTOENAD]: opphoer ? OMS_OPPHOER_BESKRIVELSE : OMS_REVURDERING_BESKRIVELSE,
  }[behandling.sakType]

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
          hjemmler={hjemmler}
          beskrivelse={beskrivelse}
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
