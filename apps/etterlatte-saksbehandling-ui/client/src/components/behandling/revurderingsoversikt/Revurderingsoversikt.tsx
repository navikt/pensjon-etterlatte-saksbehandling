import { Content, ContentHeader } from '~shared/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus, requireNotNull } from '../felles/utils'
import Virkningstidspunkt, {
  Hjemmel,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Border, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { HeadingWrapper } from '~components/person/SakOversikt'
import {
  BP_OPPHOER_BESKRIVELSE,
  BP_OPPHOER_HJEMLER,
  BP_REVURDERING_BESKRIVELSE,
  BP_REVURDERING_HJEMLER,
  OMS_INNTEKTSENDRING_BESKRIVELSE,
  OMS_INNTEKTSENDRING_HJEMLER,
  OMS_OPPHOER_BESKRIVELSE,
  OMS_OPPHOER_HJEMLER,
  OMS_REVURDERING_BESKRIVELSE,
  OMS_REVURDERING_HJEMLER,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'
import { SakType } from '~shared/types/sak'
import { erOpphoer, Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import styled from 'styled-components'
import { GrunnForSoeskenjustering } from '~components/behandling/revurderingsoversikt/GrunnForSoeskenjustering'

const revurderingsaarsakTilTekst = (revurderingsaarsak: Revurderingsaarsak): string =>
  tekstRevurderingsaarsak[revurderingsaarsak]

const hjemlerOgBeskrivelse = (sakType: SakType, revurderingsaarsak: Revurderingsaarsak): [Array<Hjemmel>, string] => {
  switch (sakType) {
    case SakType.OMSTILLINGSSTOENAD:
      return hjemlerOgBeskrivelseOmstillingsstoenad(revurderingsaarsak)
    case SakType.BARNEPENSJON:
      return hjemlerOgBeskrivelseBarnepensjon(revurderingsaarsak)
  }
}

const hjemlerOgBeskrivelseOmstillingsstoenad = (revurderingsaarsak: Revurderingsaarsak): [Array<Hjemmel>, string] => {
  switch (revurderingsaarsak) {
    case Revurderingsaarsak.DOEDSFALL:
      return [OMS_OPPHOER_HJEMLER, OMS_OPPHOER_BESKRIVELSE]
    case Revurderingsaarsak.INNTEKTSENDRING:
      return [OMS_INNTEKTSENDRING_HJEMLER, OMS_INNTEKTSENDRING_BESKRIVELSE]
    case Revurderingsaarsak.SIVILSTAND:
      return [OMS_OPPHOER_HJEMLER, OMS_OPPHOER_BESKRIVELSE]
    default:
      return [OMS_REVURDERING_HJEMLER, OMS_REVURDERING_BESKRIVELSE]
  }
}

const hjemlerOgBeskrivelseBarnepensjon = (revurderingsaarsak: Revurderingsaarsak): [Array<Hjemmel>, string] => {
  switch (revurderingsaarsak) {
    case Revurderingsaarsak.DOEDSFALL:
      return [BP_OPPHOER_HJEMLER, BP_OPPHOER_BESKRIVELSE]
    default:
      return [BP_REVURDERING_HJEMLER, BP_REVURDERING_BESKRIVELSE]
  }
}

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const revurderingsaarsak = requireNotNull(
    behandling.revurderingsaarsak,
    'Kan ikke starte en revurdering uten en revurderingsårsak'
  )

  const [hjemler, beskrivelse] = hjemlerOgBeskrivelse(behandling.sakType, revurderingsaarsak)
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Revurdering
          </Heading>
        </HeadingWrapper>
        <BodyShort>
          {erOpphoer(revurderingsaarsak) ? 'Opphør' : 'Revurdering'} på grunn av{' '}
          <Lowercase>{revurderingsaarsakTilTekst(revurderingsaarsak)}</Lowercase>.
        </BodyShort>
      </ContentHeader>
      <Innhold>
        {behandling.revurderingsaarsak === Revurderingsaarsak.SOESKENJUSTERING ? (
          <GrunnForSoeskenjustering behandling={behandling} />
        ) : null}
        <Virkningstidspunkt
          redigerbar={behandles}
          virkningstidspunkt={behandling.virkningstidspunkt}
          avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
          avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
          soeknadMottattDato={behandling.soeknadMottattDato}
          behandlingId={behandling.id}
          hjemmler={hjemler}
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
