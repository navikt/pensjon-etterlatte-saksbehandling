import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import { Aldersgruppe, IBrevutfallOgEtterbetaling } from '~components/behandling/brevutfall/Brevutfall'
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

function formaterDatoSomMaaned(dato: string) {
  return format(parseISO(dato), 'MMMM yyyy', { locale: nb })
}

export const BrevutfallVisning = (props: {
  redigerbar: boolean
  brevutfallOgEtterbetaling: IBrevutfallOgEtterbetaling
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
