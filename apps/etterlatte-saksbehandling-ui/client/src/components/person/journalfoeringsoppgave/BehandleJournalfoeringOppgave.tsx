import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { Route, Routes, useParams } from 'react-router-dom'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import VelgJournalpost from '~components/person/journalfoeringsoppgave/VelgJournalpost'
import styled from 'styled-components'
import {
  settBruker,
  settJournalpost,
  settNyBehandlingRequest,
  settOppgave,
  settSak,
} from '~store/reducers/JournalfoeringOppgaveReducer'
import { hentOppgave } from '~shared/api/oppgaver'
import { useAppDispatch } from '~store/Store'
import Spinner from '~shared/Spinner'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { isPending, isPendingOrInitial, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { OppdaterJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/OppdaterJournalpost'
import StartOppgavebehandling, {
  OppgaveDetaljer,
} from '~components/person/journalfoeringsoppgave/handling/StartOppgavebehandling'
import OpprettNyBehandling from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import OppsummeringOppgavebehandling from '~components/person/journalfoeringsoppgave/nybehandling/OppsummeringOppgavebehandling'
import FerdigstillOppgave from '~components/person/journalfoeringsoppgave/ferdigstilloppgave/FerdigstillOppgave'
import { kanEndreJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import OpprettKlagebehandling from '~components/person/journalfoeringsoppgave/oppretteklage/OpprettKlagebehandling'
import OppsummeringKlagebehandling from '~components/person/journalfoeringsoppgave/oppretteklage/OppsummeringKlagebehandling'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { hentJournalpost } from '~shared/api/dokument'
import { JournalpostInnhold } from './journalpost/JournalpostInnhold'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Alert, Box, HStack } from '@navikt/ds-react'
import { StickyToppMeny } from '~shared/StickyToppMeny'
import { logger } from '~utils/logger'

export default function BehandleJournalfoeringOppgave() {
  useSidetittel('Journalføringsoppgave')

  const { nyBehandlingRequest, oppgave, sakMedBehandlinger } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const [oppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const [sakStatus, apiHentSak] = useApiCall(hentSakMedBehandlnger)
  const [journalpostStatus, apiHentJournalpost] = useApiCall(hentJournalpost)

  const { id: oppgaveId } = useParams()

  useEffect(() => {
    if (oppgaveId && oppgave?.id !== oppgaveId) {
      apiHentOppgave(oppgaveId, (oppgave) => {
        dispatch(settOppgave(oppgave))
        dispatch(settBruker(oppgave.fnr!!))
        dispatch(
          settNyBehandlingRequest({
            ...nyBehandlingRequest,
            sakType: oppgave.sakType,
            spraak: undefined,
            mottattDato: '',
            persongalleri: { ...nyBehandlingRequest?.persongalleri, soeker: oppgave.fnr!! },
          })
        )
        apiHentSak(oppgave.fnr!!, (sak) => {
          dispatch(settSak(sak))
        })
        if (oppgave?.referanse) {
          apiHentJournalpost(oppgave.referanse, (journalpost) => dispatch(settJournalpost(journalpost)))
        } else {
          logger.generalWarning({
            msg: `oppgaveId: ${oppgaveId} mangler referanse. Sakid: ${sakMedBehandlinger?.sak.id}`,
          })
        }
      })
    }
  }, [oppgaveId])

  if (isPendingOrInitial(oppgaveStatus)) {
    return <Spinner label="Henter oppgavedetaljer..." />
  } else if (isPending(sakStatus)) {
    return <Spinner label="Henter sak..." />
  }

  if (!oppgave?.referanse) {
    return <Alert variant="error">Oppgave mangler referanse</Alert>
  }

  return (
    <>
      <StickyToppMeny>
        <StatusBar ident={oppgave?.fnr} />
        <NavigerTilbakeMeny to="/">Tilbake til oppgavebenken</NavigerTilbakeMeny>
      </StickyToppMeny>

      <HStack height="100%" minHeight="100vh" wrap={false}>
        <Box padding="8" minWidth="40rem">
          {mapResult(journalpostStatus, {
            pending: <Spinner label="Laster journalpost..." />,
            success: (journalpost) =>
              kanEndreJournalpost(journalpost) ? (
                <OppdaterJournalpost initialJournalpost={journalpost} oppgaveId={oppgaveId!!} />
              ) : (
                <Routes>
                  <Route index element={<StartOppgavebehandling />} />

                  <Route path="nybehandling">
                    <Route index element={<OpprettNyBehandling />} />
                    <Route path="oppsummering" element={<OppsummeringOppgavebehandling />} />
                  </Route>
                  <Route path="oppretteklage">
                    <Route index element={<OpprettKlagebehandling />} />
                    <Route path="oppsummering" element={<OppsummeringKlagebehandling />} />
                  </Route>

                  <Route path="ferdigstill" element={<FerdigstillOppgave />} />
                </Routes>
              ),
          })}
        </Box>

        <Box minWidth="50rem" width="100%" borderWidth="0 1" borderColor="border-subtle">
          <VelgJournalpost journalpostStatus={journalpostStatus} />
        </Box>

        <Sidebar>
          {oppgave && <OppgaveDetaljer oppgave={oppgave} />}

          {mapSuccess(
            journalpostStatus,
            (journalpost) =>
              !kanEndreJournalpost(journalpost) && (
                <SidebarPanel $border>
                  <JournalpostInnhold journalpost={journalpost} />
                </SidebarPanel>
              )
          )}
        </Sidebar>
      </HStack>
    </>
  )
}

export const FormWrapper = styled.div<{ $column?: boolean }>`
  display: flex;
  flex-direction: ${(props) => (props.$column ? 'column' : 'row')};
  gap: 1rem;
`
