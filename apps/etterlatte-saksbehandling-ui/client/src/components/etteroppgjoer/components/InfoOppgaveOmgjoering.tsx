import { BodyShort, Box, HelpText, HStack, Label, LocalAlert, VStack } from '@navikt/ds-react'
import React from 'react'

export default function InfoOppgaveOmgjoering() {
  return (
    <Box marginBlock="space-32 space-0">
      <LocalAlert status="warning">
        <LocalAlert.Header>
          <LocalAlert.Title>
            Hvis tidligere etteroppgjør viste feilutbetaling, så må det sendes oppgave til NØP
          </LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          <VStack gap="space-16">
            <HStack gap="space-4">
              Lag oppgave i Gosys med følgende:
              <HelpText title="Hvor kommer dette fra?">
                NØP må få informasjon om hva omgjøringen gjelder når det første etteroppgjøret viste feilutbetaling av
                omstillingsstønad.
              </HelpText>
            </HStack>
            <VStack gap="space-4">
              <VStack>
                <Label>Tema</Label>
                <BodyShort>Økonomi</BodyShort>
              </VStack>
              <VStack>
                <Label>Gjelder</Label>
                <BodyShort>Omstillingsstønad</BodyShort>
              </VStack>
              <VStack>
                <Label>Oppgavetype</Label>
                <BodyShort>Stopp</BodyShort>
              </VStack>
              <VStack>
                <Label>Prioritet</Label>
                <BodyShort>Haster</BodyShort>
              </VStack>
            </VStack>
            <VStack gap="space-8">
              Beskrivelse i oppgaven til NØP: Etterbetaling iht nytt vedtak må ikke utbetales pga avregning EO
              omstillingsstønad.
              <VStack>Sendes til enhet 4819.</VStack>
            </VStack>
          </VStack>
        </LocalAlert.Content>
      </LocalAlert>
    </Box>
  )
}
