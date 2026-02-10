import React, { useState } from 'react'
import { Alert, Button, HStack, TextField, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import {
  AvsenderMottaker,
  AvsenderMottakerIdType,
  BrukerIdType,
  Journalpost,
  Sakstype,
} from '~shared/types/Journalpost'
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
import { ClickEvent, trackClick } from '~utils/analytics'
import { ApiError } from '~shared/api/apiClient'
import { ApiErrorAlert } from '~ErrorBoundary'

function reparerIdTypeAvsenderMottaker(avsenderMottaker?: AvsenderMottaker): AvsenderMottaker | undefined {
  if (!avsenderMottaker) {
    return undefined
  }
  if (!avsenderMottaker.id || !!avsenderMottaker.idType) {
    return avsenderMottaker
  }
  const id = avsenderMottaker.id
  if (id.length === 9) {
    return {
      ...avsenderMottaker,
      idType: AvsenderMottakerIdType.ORGNR,
    }
  } else if (id.length === 11) {
    return {
      ...avsenderMottaker,
      idType: AvsenderMottakerIdType.FNR,
    }
  } else {
    return avsenderMottaker
  }
}

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
    trackClick(ClickEvent.FLYTT_JOURNALPOST)

    apiOppdaterJournalpost(
      {
        journalpost: {
          ...journalpost,
          avsenderMottaker: reparerIdTypeAvsenderMottaker(journalpost.avsenderMottaker),
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

        <HStack gap="space-4" justify="end">
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

  const feilkodehaandtering = (error: ApiError) => {
    switch (error.status) {
      case 404:
        return <Alert variant="warning">Fant ikke sak {sakid}</Alert>
      case 403:
        return <Alert variant="error">Du mangler tilgang til saken {error.detail}</Alert>
      default:
        return <Alert variant="error">Ukjent feil ved henting av sak {sakid}</Alert>
    }
  }

  return mapResult(annenSakStatus, {
    initial: (
      <HStack gap="space-4" align="end">
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
          icon={<MagnifyingGlassIcon aria-hidden />}
        >
          Søk
        </Button>
      </HStack>
    ),
    pending: <Spinner label="Henter sak..." />,
    success: (annenSak) => (
      <VStack gap="space-4">
        {mapSuccess(sakStatus, (data) => (
          <SakOverfoeringDetailjer fra={data.sak} til={annenSak} />
        ))}
        {mapFailure(oppdaterResult, (error) => (
          <ApiErrorAlert>
            Kunne ikke flytte journalpost til sak {annenSak.id}, på grunn av feil: {error.detail}
          </ApiErrorAlert>
        ))}

        <HStack gap="space-4" justify="end">
          <Button variant="secondary" onClick={lukkModal} disabled={isLoading}>
            Nei, avbryt
          </Button>
          <Button onClick={() => flyttJournalpost(annenSak)} loading={isLoading}>
            Flytt journalpost til sak {annenSak.id}
          </Button>
        </HStack>
      </VStack>
    ),
    error: (error) => feilkodehaandtering(error),
  })
}
