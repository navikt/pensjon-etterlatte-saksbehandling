import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import {
  Aldersgruppe,
  BrevutfallOgEtterbetaling,
  FeilutbetalingValg,
  LavEllerIngenInntekt,
} from '~components/behandling/brevutfall/Brevutfall'
import { format, parseISO } from 'date-fns'
import nb from 'date-fns/locale/nb'
import { SakType } from '~shared/types/sak'
import { PencilIcon } from '@navikt/aksel-icons'

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

function lavEllerIngenInntektToString(lavEllerIngenInntekt?: LavEllerIngenInntekt | null) {
  switch (lavEllerIngenInntekt) {
    case LavEllerIngenInntekt.JA:
      return 'Ja'
    case LavEllerIngenInntekt.NEI:
      return 'Nei'
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
    case FeilutbetalingValg.JA_INGEN_TK:
      return 'Ja, men under 4 rettsgebyr, så ingen tilbakekreving'
    default:
      return 'Ikke satt'
  }
}

function formaterDatoSomMaaned(dato: string) {
  return format(parseISO(dato), 'MMMM yyyy', { locale: nb })
}

export const BrevutfallVisning = (props: {
  redigerbar: boolean
  brevutfallOgEtterbetaling: BrevutfallOgEtterbetaling
  sakType: SakType
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const { redigerbar, brevutfallOgEtterbetaling, sakType, setVisSkjema } = props

  return (
    <VStack gap="8">
      <VStack gap="2">
        <VStack gap="2">
          <Label>Skal det etterbetales?</Label>
          <BodyShort>{brevutfallOgEtterbetaling.etterbetaling ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
        {brevutfallOgEtterbetaling.etterbetaling && (
          <HStack gap="8">
            <VStack gap="2">
              <Label>Fra og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevutfallOgEtterbetaling.etterbetaling.datoFom!!)}</BodyShort>
            </VStack>
            <VStack gap="2">
              <Label>Til og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevutfallOgEtterbetaling.etterbetaling.datoTom!!)}</BodyShort>
            </VStack>
          </HStack>
        )}
      </VStack>
      {sakType == SakType.BARNEPENSJON && (
        <VStack gap="2">
          <Label>Gjelder brevet under eller over 18 år?</Label>
          <BodyShort>{aldersgruppeToString(brevutfallOgEtterbetaling.brevutfall.aldersgruppe)}</BodyShort>
        </VStack>
      )}
      {sakType == SakType.OMSTILLINGSSTOENAD && (
        <VStack gap="2">
          <Label>Gi omstillingsstønad til 67 år etter unntaksregel for bruker født tom 1963?</Label>
          <BodyShort>
            {lavEllerIngenInntektToString(brevutfallOgEtterbetaling.brevutfall.lavEllerIngenInntekt)}
          </BodyShort>
        </VStack>
      )}

      {brevutfallOgEtterbetaling.brevutfall.feilutbetaling && (
        <VStack gap="2">
          <Label>Medfører revurderingen en feilutbetaling?</Label>
          <BodyShort>{feilutbetalingToString(brevutfallOgEtterbetaling.brevutfall.feilutbetaling.valg)}</BodyShort>
          <BodyShort>{brevutfallOgEtterbetaling.brevutfall.feilutbetaling.kommentar}</BodyShort>
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
