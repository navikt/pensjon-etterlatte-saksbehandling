import { Alert, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import React from 'react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'
import { SettPaaVentModal } from '~components/behandling/sidemeny/SettPaaVentModal'

interface Props {
  oppgave: OppgaveDTO | null
}

export const SettPaaVent = ({ oppgave }: Props) => {
  if (!oppgave || !erOppgaveRedigerbar(oppgave?.status)) return null

  return (
    <VStack gap="space-4">
      {oppgave?.status === 'PAA_VENT' && (
        <>
          <Alert variant="warning" size="small">
            <Heading size="xsmall" spacing>
              Oppgaven står på vent!
            </Heading>
            <VStack gap="space-4">
              <Info label="Merknad" tekst={oppgave.merknad || 'Ingen'} />
              <Info label="Ny frist" tekst={formaterDato(oppgave.frist)} />
            </VStack>
          </Alert>
        </>
      )}

      <HStack justify="end">
        <SettPaaVentModal oppgave={oppgave} />
      </HStack>
    </VStack>
  )
}
