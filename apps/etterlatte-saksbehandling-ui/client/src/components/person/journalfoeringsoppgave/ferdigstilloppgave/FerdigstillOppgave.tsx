import { Alert, BodyLong, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, NavLink, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FerdigstillOppgaveModal from '~components/person/journalfoeringsoppgave/ferdigstilloppgave/FerdigstillOppgaveModal'
import { Journalstatus } from '~shared/types/Journalpost'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import React from 'react'

export default function FerdigstillOppgave() {
  const { journalpost, oppgave } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (!oppgave || !erOppgaveRedigerbar(oppgave.status)) {
    return <Navigate to="../" relative="path" />
  }

  const journalpostErFerdigstilt =
    journalpost &&
    [Journalstatus.FERDIGSTILT, Journalstatus.JOURNALFOERT, Journalstatus.FEILREGISTRERT].includes(
      journalpost.journalstatus
    )

  const journalpostTilhoererAnnetTema = journalpost && !['EYO', 'EYB'].includes(journalpost.tema)
  const kanFerdigstilleOppgaven = journalpostErFerdigstilt || journalpostTilhoererAnnetTema

  return (
    <FormWrapper $column>
      <Heading size="medium" spacing>
        Ferdigstill oppgave
      </Heading>

      {journalpost ? (
        <>
          <BodyLong spacing>
            Dersom journalposten allerede er ferdigstilt og/eller ikke relevant, kan oppgaven ferdigstilles.
          </BodyLong>

          {!kanFerdigstilleOppgaven && (
            <Alert variant="warning">
              Journalposten har status {journalpost?.journalstatus}. Oppgaven kan bare ferdigstilles når journalposten
              er ferdig behandlet.
            </Alert>
          )}
        </>
      ) : (
        <Alert variant="warning">
          Journalposten har ikke blitt lastet inn som forventet. Er du helt sikker på at du vil ferdigstille oppgaven?
          <br /> Du kan kontrollere dokumentent i{' '}
          <NavLink to={`/person?fane=${PersonOversiktFane.DOKUMENTER}`} state={{ fnr: oppgave.fnr }} target="_blank">
            dokumentoversikten <ExternalLinkIcon />
          </NavLink>
        </Alert>
      )}

      <VStack gap="2">
        <HStack gap="4" justify="center">
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FerdigstillOppgaveModal oppgave={oppgave} />
        </HStack>
        <HStack gap="4" justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </HStack>
      </VStack>
    </FormWrapper>
  )
}
