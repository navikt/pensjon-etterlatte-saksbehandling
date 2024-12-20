import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import { Aldersgruppe, Brevutfall, FeilutbetalingValg } from '~components/behandling/brevutfall/Brevutfall'
import { SakType } from '~shared/types/sak'
import { PencilIcon } from '@navikt/aksel-icons'
import { hasValue } from '~components/behandling/felles/utils'

function aldersgruppeToString(aldersgruppe?: Aldersgruppe | null) {
  switch (aldersgruppe) {
    case Aldersgruppe.OVER_18:
      return 'Over 18 år'
    case Aldersgruppe.UNDER_18:
      return 'Under 18 år'
    default:
      return 'Ikke satt'
  }
}

export function feilutbetalingToString(feilutbetaling?: FeilutbetalingValg | null) {
  switch (feilutbetaling) {
    case FeilutbetalingValg.NEI:
      return 'Nei'
    case FeilutbetalingValg.JA_VARSEL:
      return 'Ja, det skal sendes varsel'
    case FeilutbetalingValg.JA_INGEN_VARSEL_MOTREGNES:
      return 'Ja, men motregnes mot annen ytelse fra NAV, så avventer varsel om feilutbetaling til ev. tilbakekrevingssak'
    case FeilutbetalingValg.JA_INGEN_TK:
      return 'Ja, men ikke grunnlag for tilbakekreving pga under 4 rettsgebyr, eller annen grunn (se notat)'
    default:
      return 'Ikke satt'
  }
}

export const BrevutfallVisning = (props: {
  behandlingErOpphoer: boolean
  redigerbar: boolean
  brevutfall: Brevutfall
  sakType: SakType
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const { behandlingErOpphoer, redigerbar, brevutfall, sakType, setVisSkjema } = props

  return (
    <VStack gap="8">
      {!behandlingErOpphoer && (
        <VStack gap="2">
          <VStack gap="2">
            <Label>Skal det etterbetales?</Label>
            <BodyShort>{brevutfall.harEtterbetaling ? 'Ja' : 'Nei'}</BodyShort>
          </VStack>

          {sakType === SakType.BARNEPENSJON && hasValue(brevutfall?.frivilligSkattetrekk) && (
            <VStack gap="2">
              <Label>Har bruker meldt inn frivillig skattetrekk utover 17%?</Label>
              <BodyShort>{brevutfall.frivilligSkattetrekk ? 'Ja' : 'Nei'}</BodyShort>
            </VStack>
          )}
        </VStack>
      )}
      {sakType === SakType.BARNEPENSJON && (
        <VStack gap="2">
          <Label>Gjelder brevet under eller over 18 år?</Label>
          <BodyShort>{aldersgruppeToString(brevutfall.aldersgruppe)}</BodyShort>
        </VStack>
      )}

      {brevutfall.feilutbetaling && (
        <VStack gap="2">
          <Label>Medfører revurderingen en feilutbetaling?</Label>
          <BodyShort>{feilutbetalingToString(brevutfall.feilutbetaling.valg)}</BodyShort>
          <Label>Kommentar</Label>
          <BodyShort>{brevutfall.feilutbetaling.kommentar}</BodyShort>
        </VStack>
      )}
      {redigerbar && (
        <HStack>
          <Button
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            size="small"
            onClick={() => setVisSkjema(true)}
          >
            Rediger
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
