/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Alert, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React, { useEffect, useState } from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { formaterDato } from '~utils/formatering/dato'
import { formaterJournalpostStatus } from '~utils/formatering/formatering'
import styled from 'styled-components'
import { EndreTema } from '~components/person/journalfoeringsoppgave/journalpost/EndreTema'
import { EndreBruker } from '~components/person/journalfoeringsoppgave/journalpost/EndreBruker'
import { EndreAvsenderMottaker } from '~components/person/journalfoeringsoppgave/journalpost/EndreAvsenderMottaker'
import { EndreSak } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { EndreDokumenter } from '~components/person/journalfoeringsoppgave/journalpost/EndreDokumenter'
import JournalfoerJournalpostModal from '~components/person/journalfoeringsoppgave/journalpost/modal/JournalfoerJournalpostModal'
import LagreJournalpostModal from '~components/person/journalfoeringsoppgave/journalpost/modal/LagreJournalpostModal'
import { EndreTittelJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/EndreTittelJournalpost'
import { useAppSelector } from '~store/Store'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ISak } from '~shared/types/sak'
import { fnrHarGyldigFormat } from '~utils/fnr'

interface Props {
  initialJournalpost: Journalpost
  sak: ISak
  oppgaveId: string
}

export const OppdaterJournalpost = ({ initialJournalpost, sak, oppgaveId }: Props) => {
  const [journalpost, setJournalpost] = useState<Journalpost>({ ...initialJournalpost })
  const { sakMedBehandlinger } = useAppSelector((store) => store.journalfoeringOppgaveReducer)
  const [sakStatus, apiHentSak] = useApiCall(hentSakMedBehandlnger)

  useEffect(() => {
    const identFraJournalpost = journalpost.bruker?.id

    if (!!identFraJournalpost && sak.ident !== identFraJournalpost && fnrHarGyldigFormat(identFraJournalpost)) {
      apiHentSak(identFraJournalpost)
    } else {
      apiHentSak(sak.ident)
    }
  }, [journalpost.bruker?.id, sak.ident])

  return (
    <>
      <Heading size="medium" spacing>
        Journalføringsoppgave
      </Heading>

      <Alert variant="info">
        Journalpost må ferdigstilles eller overføres til annet tema før selve oppgaven kan behandles
      </Alert>

      <br />

      <VStack gap="space-4">
        <Info label="Kanal/kilde" tekst={journalpost.kanal} />
        <Info
          label="Registrert dato"
          tekst={journalpost.datoOpprettet ? formaterDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
        />
        <Info label="Status" tekst={formaterJournalpostStatus(journalpost.journalstatus)} />
      </VStack>

      <br />

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        <Heading size="medium" spacing>
          Gjelder
        </Heading>
      </Box>

      <FormWrapper $column={true}>
        <EndreTema journalpost={journalpost} oppdater={(kode) => setJournalpost({ ...journalpost, tema: kode.term })} />

        <EndreBruker
          bruker={journalpost.bruker}
          oppdaterBruker={(bruker) => setJournalpost({ ...journalpost, bruker })}
        />

        <EndreTittelJournalpost
          journalpost={journalpost}
          oppdaterTittelJournalpost={(nyTittel) => setJournalpost({ ...journalpost, tittel: nyTittel })}
        />

        <EndreAvsenderMottaker
          key={journalpost.avsenderMottaker.id}
          avsenderMottaker={journalpost.avsenderMottaker}
          oppdaterAvsenderMottaker={(avsenderMottaker) => setJournalpost({ ...journalpost, avsenderMottaker })}
        />

        <EndreDokumenter
          initielleDokumenter={journalpost.dokumenter}
          oppdater={(dokumenter) => setJournalpost({ ...journalpost, dokumenter })}
        />

        {mapResult(sakStatus, {
          pending: <Spinner label="Laster sak..." />,
          error: (
            <Alert variant="error" size="small">
              Kunne ikke hente sak for {journalpost.bruker?.id}, sjekk at dette er et gyldig fødselsnummer. Du kan endre
              mottaker og laste saker på nytt.
            </Alert>
          ),
          success: (sakMedBehandling) => (
            <>
              <EndreSak
                fagsak={journalpost.sak}
                gjennySak={sakMedBehandling.sak}
                alternativSak={sakMedBehandlinger?.ekstraSak?.sak}
                kobleTilSak={(nySak) => setJournalpost({ ...journalpost, sak: nySak })}
              />

              <VStack gap="space-2">
                <HStack gap="space-2" justify="center">
                  <LagreJournalpostModal journalpost={journalpost} oppgaveId={oppgaveId} />
                  <JournalfoerJournalpostModal journalpost={journalpost} sak={sakMedBehandling.sak} />
                </HStack>
                <HStack justify="center">
                  <AvbrytBehandleJournalfoeringOppgave />
                </HStack>
              </VStack>
            </>
          ),
        })}
      </FormWrapper>
    </>
  )
}

export const InputFlexRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 0.5rem;

  > :not(:last-child) {
    flex: 1;
  }
`
