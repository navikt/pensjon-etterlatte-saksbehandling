import { BodyShort, Box, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErRedigerbar, requireNotNull } from '../felles/utils'
import Virkningstidspunkt, { Hjemmel } from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { Start } from '~components/behandling/handlinger/Start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { HeadingWrapper } from '~components/person/sakOgBehandling/SakOversikt'
import {
  BP_FORELDRELOES_BESKRIVELSE,
  BP_FORELDRELOES_HJEMLER,
  BP_INSTITUSJONSOPPHOLD_BESKRIVELSE,
  BP_INSTITUSJONSOPPHOLD_HJEMLER,
  BP_OPPHOER_BESKRIVELSE,
  BP_OPPHOER_HJEMLER,
  BP_REVURDERING_BESKRIVELSE,
  BP_REVURDERING_YRKESSKADE_BESKRIVELSE,
  BP_REVURDERING_YRKESSKADE_HJEMLER,
  FELLES_REVURDERING_HJEMLER,
  FELLES_SLUTTBEHANDLING_BESKRIVELSE,
  FELLES_SLUTTBEHANDLING_HJEMLER,
  OMS_ALDERSOVERGANG_BESKRIVELSE,
  OMS_ALDERSOVERGANG_HJEMLER,
  OMS_INNTEKTSENDRING_BESKRIVELSE,
  OMS_INNTEKTSENDRING_HJEMLER,
  OMS_INST_HJEMLER_VIRK,
  OMS_INST_VIRK_BESKRIVELSE,
  OMS_OPPHOER_BESKRIVELSE,
  OMS_OPPHOER_HJEMLER,
  OMS_REVURDERING_BESKRIVELSE,
} from '~components/behandling/virkningstidspunkt/utils'
import { SakType } from '~shared/types/sak'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import styled from 'styled-components'
import { GrunnForSoeskenjustering } from '~components/behandling/revurderingsoversikt/GrunnForSoeskenjustering'
import { GrunnlagForVirkningstidspunkt } from '~components/behandling/revurderingsoversikt/GrunnlagForVirkningstidspunkt'
import { RevurderingAnnen } from '~components/behandling/revurderingsoversikt/RevurderingAnnen'
import SluttbehandlingUtland from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtland'
import { SluttbehandlingUtlandInfo } from '~shared/types/RevurderingInfo'
import OppdaterGrunnlagModal from '~components/behandling/handlinger/OppdaterGrunnlagModal'
import { Utlandstilknytning } from '~components/behandling/soeknadsoversikt/utlandstilknytning/Utlandstilknytning'
import { Familieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/Familieforhold'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { RevurderingKravpakke } from '~components/behandling/revurderingsoversikt/RevurderingKravpakke'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { ViderefoereOpphoer } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoer'

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
    case Revurderingaarsak.ALDERSOVERGANG:
      return [OMS_ALDERSOVERGANG_HJEMLER, OMS_ALDERSOVERGANG_BESKRIVELSE]
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
    case Revurderingaarsak.FORELDRELOES:
      return [BP_FORELDRELOES_HJEMLER, BP_FORELDRELOES_BESKRIVELSE]
    default:
      return [FELLES_REVURDERING_HJEMLER, BP_REVURDERING_BESKRIVELSE]
  }
}

export const Revurderingsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const revurderingsaarsak = requireNotNull(
    behandling.revurderingsaarsak,
    'Kan ikke starte en revurdering uten en revurderingsårsak'
  )
  const personopplysninger = usePersonopplysninger()

  const [hjemler, beskrivelse] = hjemlerOgBeskrivelse(behandling.sakType, revurderingsaarsak)
  return (
    <>
      <Box paddingInline="16" paddingBlock="4">
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Revurdering
          </Heading>
        </HeadingWrapper>
        {[Revurderingaarsak.ANNEN, Revurderingaarsak.ANNEN_UTEN_BREV].includes(revurderingsaarsak) ? (
          <BodyShort spacing>Revurdering på grunn av annen årsak (spesifiseres nedenfor).</BodyShort>
        ) : (
          <BodyShort spacing>
            Revurdering på grunn av <Lowercase>{revurderingsaarsakTilTekst(revurderingsaarsak)}</Lowercase>.
          </BodyShort>
        )}
      </Box>
      <Box paddingBlock="8" paddingInline="16 8">
        <OppdaterGrunnlagModal
          behandlingId={behandling.id}
          behandlingStatus={behandling.status}
          enhetId={behandling.sakEnhetId}
        />
        <Utlandstilknytning behandling={behandling} redigerbar={redigerbar} />
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
        {behandling.revurderingsaarsak === Revurderingaarsak.ANNEN && (
          <RevurderingAnnen type={Revurderingaarsak.ANNEN} behandling={behandling} />
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.ANNEN_UTEN_BREV && (
          <RevurderingAnnen type={Revurderingaarsak.ANNEN_UTEN_BREV} behandling={behandling} />
        )}
        {behandling.revurderingsaarsak === Revurderingaarsak.UTSENDELSE_AV_KRAVPAKKE && (
          <RevurderingKravpakke behandling={behandling} />
        )}

        <Virkningstidspunkt
          erBosattUtland={false}
          redigerbar={redigerbar}
          behandling={behandling}
          hjemler={hjemler}
          beskrivelse={beskrivelse}
        >
          {{ info: <GrunnlagForVirkningstidspunkt /> }}
        </Virkningstidspunkt>

        {useFeatureEnabledMedDefault('viderefoer-opphoer', false) && behandling.virkningstidspunkt && (
          <ViderefoereOpphoer behandling={behandling} redigerbar={redigerbar} />
        )}
      </Box>
      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <Familieforhold behandling={behandling} personopplysninger={personopplysninger} redigerbar={redigerbar} />
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Start disabled={behandling.virkningstidspunkt === null} />
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}

const Lowercase = styled.span`
  text-transform: lowercase;
`
