import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import { Aldersgruppe, Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { format } from 'date-fns'
import nb from 'date-fns/locale/nb'

function aldersgruppeToString(aldersgruppe: Aldersgruppe) {
  switch (aldersgruppe) {
    case Aldersgruppe.OVER_18:
      return 'Over 18 år'
    case Aldersgruppe.UNDER_18:
      return 'Under 18 år'
  }
}

function formaterDatoSomMaaned(dato: Date) {
  return format(dato, 'MMMM yyyy', { locale: nb })
}

export const BrevutfallVisning = (props: {
  redigerbar: boolean
  brevutfall: Brevutfall
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const { redigerbar, brevutfall, setVisSkjema } = props

  return (
    <VStack gap="8">
      <VStack gap="2">
        <VStack gap="2">
          <Label>Skal det etterbetales?</Label>
          <BodyShort>{brevutfall.etterbetaling ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
        {brevutfall.etterbetaling && (
          <HStack gap="8">
            <VStack gap="2">
              <Label>Fra og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevutfall.etterbetaling.fom!!)}</BodyShort>
            </VStack>
            <VStack gap="2">
              <Label>Til og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevutfall.etterbetaling.fom!!)}</BodyShort>
            </VStack>
          </HStack>
        )}
      </VStack>
      <VStack gap="2">
        <Label>Gjelder brevet under eller over 18 år?</Label>
        <BodyShort>{brevutfall.aldersgruppe ? aldersgruppeToString(brevutfall.aldersgruppe) : ''}</BodyShort>
      </VStack>
      {redigerbar && (
        <HStack>
          <Button size="small" onClick={() => setVisSkjema(true)}>
            Rediger
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
