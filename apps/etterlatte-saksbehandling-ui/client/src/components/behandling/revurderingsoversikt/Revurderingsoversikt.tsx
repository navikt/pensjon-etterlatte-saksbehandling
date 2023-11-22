import { Content, ContentHeader } from '~shared/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErRedigerbar, requireNotNull } from '../felles/utils'
import Virkningstidspunkt, { Hjemmel } from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Border, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { HeadingWrapper } from '~components/person/SakOversikt'
import {
  BP_INSTITUSJONSOPPHOLD_BESKRIVELSE,
  BP_INSTITUSJONSOPPHOLD_HJEMLER,
  FELLES_SLUTTBEHANDLING_BESKRIVELSE,
  BP_OPPHOER_BESKRIVELSE,
  BP_OPPHOER_HJEMLER,
  BP_REVURDERING_BESKRIVELSE,
  FELLES_REVURDERING_HJEMLER,
  BP_REVURDERING_YRKESSKADE_BESKRIVELSE,
  BP_REVURDERING_YRKESSKADE_HJEMLER,
  OMS_INNTEKTSENDRING_BESKRIVELSE,
  OMS_INNTEKTSENDRING_HJEMLER,
  OMS_INST_HJEMLER_VIRK,
  OMS_INST_VIRK_BESKRIVELSE,
  OMS_OPPHOER_BESKRIVELSE,
  OMS_OPPHOER_HJEMLER,
  OMS_REVURDERING_BESKRIVELSE,
  FELLES_SLUTTBEHANDLING_HJEMLER,
} from '~components/behandling/virkningstidspunkt/utils'
import { SakType } from '~shared/types/sak'
import { erOpphoer, Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import styled from 'styled-components'
import { GrunnForSoeskenjustering } from '~components/behandling/revurderingsoversikt/GrunnForSoeskenjustering'
import { AdoptertAv } from '~components/behandling/revurderingsoversikt/AdoptertAv'
import { GrunnlagForVirkningstidspunkt } from '~components/behandling/revurderingsoversikt/GrunnlagForVirkningstidspunkt'
import { OmgjoeringAvFarskap } from '~components/behandling/revurderingsoversikt/OmgjoeringAvFarskap'
import { RevurderingAnnen } from '~components/behandling/revurderingsoversikt/RevurderingAnnen'
import SluttbehandlingUtland from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtland'
import { Soeknadsdato } from '~components/behandling/soeknadsoversikt/Soeknadsdato'
import { SluttbehandlingUtlandInfo } from '~shared/types/RevurderingInfo'
import OppdaterGrunnlagModal from '~components/behandling/handlinger/OppdaterGrunnlagModal'

const revurderingsaarsakTilTekst = (revurderingsaarsak: Revurderingaarsak): string =>
  tekstRevurderingsaarsak[revurderingsaarsak]

const hjemlerOgBeskrivelse = (sakType: SakType, revurderingsaarsak: Revurderingaarsak): [Array<Hjemmel>, string] => {
  switch (sakType) {
    case SakType.OMSTILLINGSSTOENAD:
      return hjemlerOgBeskrivelseOmstillingsstoenad(revurderingsaarsak)
    case SakType.BARNEPENSJON:
      return hjemlerOgBeskrivelseBarnepensjon(revurderingsaarsak)
  }
}

const hjemlerOgBeskrivelseOmstillingsstoenad = (revurderingsaarsak: Revurderingaarsak): [Array<Hjemmel>, string] => {
  switch (revurderingsaarsak) {
    case Revurderingaarsak.DOEDSFALL:
      return [OMS_OPPHOER_HJEMLER, OMS_OPPHOER_BESKRIVELSE]
    case Revurderingaarsak.INNTEKTSENDRING:
      return [OMS_INNTEKTSENDRING_HJEMLER, OMS_INNTEKTSENDRING_BESKRIVELSE]
    case Revurderingaarsak.SIVILSTAND:
      return [OMS_OPPHOER_HJEMLER, OMS_OPPHOER_BESKRIVELSE]
    case Revurderingaarsak.INSTITUSJONSOPPHOLD:
      return [OMS_INST_HJEMLER_VIRK, OMS_INST_VIRK_BESKRIVELSE]
    case Revurderingaarsak.SLUTTBEHANDLING_UTLAND:
      return [FELLES_SLUTTBEHANDLING_HJEMLER, FELLES_SLUTTBEHANDLING_BESKRIVELSE]
    default:
      return [FELLES_REVURDERING_HJEMLER, OMS_REVURDERING_BESKRIVELSE]
  }
}

const hjemlerOgBeskrivelseBarnepensjon = (revurderingsaarsak: Revurderingaarsak): [Array<Hjemmel>, string] => {
  switch (revurderingsaarsak) {
    case Revurderingaarsak.DOEDSFALL:
      return [BP_OPPHOER_HJEMLER, BP_OPPHOER_BESKRIVELSE]
    case Revurderingaarsak.YRKESSKADE:
      return [BP_REVURDERING_YRKESSKADE_HJEMLER, BP_REVURDERING_YRKESSKADE_BESKRIVELSE]
    case Revurderingaarsak.INSTITUSJONSOPPHOLD:
    case Revurderingaarsak.FENGSELSOPPHOLD: //TODO: kanskje Revurderingaarsak.UT_AV_FENGSEL: men ikke i bruk nå..
      return [BP_INSTITUSJONSOPPHOLD_HJEMLER, BP_INSTITUSJONSOPPHOLD_BESKRIVELSE]
    case Revurderingaarsak.SLUTTBEHANDLING_UTLAND:
      return [FELLES_SLUTTBEHANDLING_HJEMLER, FELLES_SLUTTBEHANDLING_BESKRIVELSE]
    default:
      return [FELLES_REVURDERING_HJEMLER, BP_REVURDERING_BESKRIVELSE]
  }
}

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const redigerbar = behandlingErRedigerbar(behandling.status)
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
        {revurderingsaarsak != Revurderingaarsak.ANNEN && (
          <BodyShort spacing>
            {erOpphoer(revurderingsaarsak) ? 'Opphør' : 'Revurdering'} på grunn av{' '}
            <Lowercase>{revurderingsaarsakTilTekst(revurderingsaarsak)}</Lowercase>.
          </BodyShort>
        )}
        <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />
      </ContentHeader>
      <InnholdPadding>
        <OppdaterGrunnlagModal behandlingId={behandling.id} behandlingStatus={behandling.status} />
        {behandling.begrunnelse !== null && (
          <>
            <Heading size="small">Begrunnelse</Heading>
            <BodyShort>{behandling.begrunnelse}</BodyShort>
          </>
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND && (
          <SluttbehandlingUtland
            sakId={behandling.sakId}
            revurderingId={behandling.id}
            sluttbehandlingUtland={behandling.revurderinginfo?.revurderingInfo as SluttbehandlingUtlandInfo | undefined}
            redigerbar={redigerbar}
          />
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.SOESKENJUSTERING && (
          <GrunnForSoeskenjustering behandling={behandling} />
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.ADOPSJON && <AdoptertAv behandling={behandling} />}
        {behandling.revurderingsaarsak === Revurderingaarsak.OMGJOERING_AV_FARSKAP && (
          <OmgjoeringAvFarskap behandling={behandling} />
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.ANNEN && <RevurderingAnnen behandling={behandling} />}

        <Virkningstidspunkt
          erBosattUtland={false}
          redigerbar={redigerbar}
          behandling={behandling}
          hjemler={hjemler}
          beskrivelse={beskrivelse}
        >
          {{ info: <GrunnlagForVirkningstidspunkt /> }}
        </Virkningstidspunkt>
      </InnholdPadding>
      <Border />
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Start disabled={behandling.virkningstidspunkt === null} />
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
