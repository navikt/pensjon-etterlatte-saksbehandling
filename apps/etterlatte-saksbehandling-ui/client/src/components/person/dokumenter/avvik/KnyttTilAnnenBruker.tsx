import React, { useState } from 'react'
import { Alert, Button, HStack, TextField } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { BrukerIdType, Journalpost, Sakstype } from '~shared/types/Journalpost'
import { ISak } from '~shared/types/sak'
import { hentSak } from '~shared/api/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakMedBehandlinger } from '~components/person/typer'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { oppdaterJournalpost } from '~shared/api/dokument'
import { MagnifyingGlassIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { useNavigate } from 'react-router-dom'
import { opprettOppgave } from '~shared/api/oppgaver'
import { SakOverfoeringDetailjer } from 'src/components/person/dokumenter/avvik/common/SakOverfoeringDetailjer'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const KnyttTilAnnenBruker = ({
  journalpost,
  sakStatus,
  lukkModal,
}: {
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
  lukkModal: () => void
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [sakid, setSakid] = useState<string>()

  const [annenSakStatus, hentAnnenSak] = useApiCall(hentSak)
  const [oppdaterResult, apiOppdaterJournalpost] = useApiCall(oppdaterJournalpost)
  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)

  const flyttJournalpost = (sak: ISak) => {
    apiOppdaterJournalpost(
      {
        journalpost: {
          ...journalpost,
          bruker: {
            id: sak.ident,
            type: BrukerIdType.FNR,
          },
          sak: {
            fagsakId: sak.id.toString(),
            fagsaksystem: 'EY',
            sakstype: Sakstype.FAGSAK,
          },
          tema: temaFraSakstype(sak.sakType),
        },
      },
      ({ journalpostId }) => {
        const oppgaveType = Oppgavetype.JOURNALFOERING

        apiOpprettOppgave({
          sakId: sak.id,
          request: {
            oppgaveType,
            referanse: journalpostId,
            merknad: `Journalpost flyttet fra bruker ${journalpost.bruker?.id}`,
            oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
            saksbehandler: innloggetSaksbehandler.ident,
          },
        })
      }
    )
  }

  if (isSuccess(oppdaterResult) && isSuccess(opprettOppgaveStatus)) {
    return (
      <>
        <Alert variant="success">Journalposten ble flyttet til sak {sakid} og journalføringsoppgave opprettet.</Alert>

        <br />

        <HStack gap="4" justify="end">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>
          {mapSuccess(opprettOppgaveStatus, (oppgave) => (
            <Button onClick={() => navigate(`/oppgave/${oppgave.id}`)}>Gå til oppgaven</Button>
          ))}
        </HStack>
      </>
    )
  }

  const isLoading = isPending(oppdaterResult) || isPending(opprettOppgaveStatus)

  return mapResult(annenSakStatus, {
    initial: (
      <HStack gap="4" align="end">
        <TextField
          label="Hvilken sakid skal journalposten flyttes til?"
          value={sakid || ''}
          type="tel"
          onChange={(e) => setSakid(e.target.value?.replace(/[^0-9+]/, ''))}
        />
        <Button
          onClick={() => hentAnnenSak(Number(sakid))}
          loading={isPending(annenSakStatus)}
          disabled={!sakid}
          icon={<MagnifyingGlassIcon />}
        >
          Søk
        </Button>
      </HStack>
    ),
    pending: <Spinner label="Henter sak..." />,
    success: (annenSak) => (
      <>
        {isSuccess(sakStatus) && <SakOverfoeringDetailjer fra={sakStatus.data.sak} til={annenSak} />}
        <br />

        <HStack gap="4" justify="end">
          <Button variant="secondary" onClick={lukkModal} disabled={isLoading}>
            Nei, avbryt
          </Button>
          <Button onClick={() => flyttJournalpost(annenSak)} loading={isLoading}>
            Flytt journalpost til sak {annenSak.id}
          </Button>
        </HStack>
      </>
    ),
    error: (error) =>
      error.status === 404 ? (
        <Alert variant="warning">Fant ikke sak {sakid}</Alert>
      ) : (
        <Alert variant="error">Ukjent feil ved henting av sak {sakid}</Alert>
      ),
  })
}
