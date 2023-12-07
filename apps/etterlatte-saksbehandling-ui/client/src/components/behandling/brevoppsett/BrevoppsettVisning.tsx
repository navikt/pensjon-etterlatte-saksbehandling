import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import { Aldersgruppe, Brevoppsett } from '~components/behandling/brevoppsett/Brevoppsett'
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

export const BrevoppsettVisning = (props: {
  redigerbar: boolean
  brevoppsett: Brevoppsett
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const { redigerbar, brevoppsett, setVisSkjema } = props

  return (
    <VStack gap="8">
      <VStack gap="2">
        <VStack gap="2">
          <Label>Skal det etterbetales?</Label>
          <BodyShort>{brevoppsett.etterbetaling ? 'Ja' : 'Nei'}</BodyShort>
        </VStack>
        {brevoppsett.etterbetaling && (
          <HStack gap="8">
            <VStack gap="2">
              <Label>Fra og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevoppsett.etterbetaling.fom!!)}</BodyShort>
            </VStack>
            <VStack gap="2">
              <Label>Til og med</Label>
              <BodyShort>{formaterDatoSomMaaned(brevoppsett.etterbetaling.fom!!)}</BodyShort>
            </VStack>
          </HStack>
        )}
      </VStack>
      <VStack gap="2">
        <Label>Gjelder brevet under eller over 18 år?</Label>
        <BodyShort>{brevoppsett.aldersgruppe ? aldersgruppeToString(brevoppsett.aldersgruppe) : ''}</BodyShort>
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
