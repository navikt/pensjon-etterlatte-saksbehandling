import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { Button, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import FullfoerKlageModal from '~components/person/journalfoeringsoppgave/oppretteklage/FullfoerKlageModal'
import { NyKlageRequestUtfylling } from '~shared/types/Klage'
import { formaterSakstype } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'

export function opprettKlageErUtfylt(
  utfylling?: Partial<NyKlageRequestUtfylling>
): utfylling is NyKlageRequestUtfylling {
  if (!utfylling) {
    return false
  }
  return !!utfylling.mottattDato
}

export default function OppsummeringKlagebehandling() {
  const { oppgave, nyKlageRequest, journalpost } = useJournalfoeringOppgave()
  const navigate = useNavigate()

  if (!oppgave || !nyKlageRequest || !journalpost) {
    return <Navigate to="../" relative="path" />
  }

  if (!opprettKlageErUtfylt(nyKlageRequest)) {
    return null
  }

  const { mottattDato } = nyKlageRequest

  const tilbake = () => navigate('../', { relative: 'path' })

  return (
    <FormWrapper $column>
      <Heading size="medium" spacing>
        Opprett klage fra oppgave{' '}
        <Tag variant="success" size="medium">
          {formaterSakstype(oppgave.sakType)}
        </Tag>
      </Heading>

      <VStack gap="space-4">
        <Info label="Klage framsatt dato" tekst={formaterDato(mottattDato)} />
      </VStack>
      <div>
        <HStack gap="space-4" justify="center">
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>
          <FullfoerKlageModal oppgave={oppgave} klageRequest={nyKlageRequest} journalpost={journalpost} />
        </HStack>

        <HStack justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </HStack>
      </div>
    </FormWrapper>
  )
}
