import React, { useState } from 'react'
import { Alert, Button, TextField } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { BrukerIdType, Journalpost, Sakstype } from '~shared/types/Journalpost'
import { ISak } from '~shared/types/sak'
import { hentSak } from '~shared/api/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakMedBehandlinger } from '~components/person/typer'
import { FlexRow } from '~shared/styled'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { oppdaterJournalpost } from '~shared/api/dokument'
import { MagnifyingGlassIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { useNavigate } from 'react-router-dom'
import { opprettOppgave, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { useAppSelector } from '~store/Store'
import { SakOverfoeringDetailjer } from 'src/components/person/dokumenter/avvik/common/SakOverfoeringDetailjer'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'

export const KnyttTilAnnentBruker = ({
  journalpost,
  sakStatus,
  lukkModal,
}: {
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
  lukkModal: () => void
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [sakid, setSakid] = useState<string>()

  const [annenSakStatus, hentAnnenSak] = useApiCall(hentSak)
  const [oppdaterResult, apiOppdaterJournalpost] = useApiCall(oppdaterJournalpost)
  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)
  const [tildelSaksbehandlerStatus, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

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

        apiOpprettOppgave(
          {
            sakId: sak.id,
            request: {
              oppgaveType,
              referanse: journalpostId,
              merknad: `Journalpost flyttet fra bruker ${journalpost.bruker?.id}`,
              oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
            },
          },
          (oppgave) => {
            tildelSaksbehandler({
              oppgaveId: oppgave.id,
              type: oppgaveType,
              nysaksbehandler: { saksbehandler: innloggetSaksbehandler.ident, versjon: null },
            })
          }
        )
      }
    )
  }

  if (isSuccess(oppdaterResult) && isSuccess(opprettOppgaveStatus) && isSuccess(tildelSaksbehandlerStatus)) {
    return (
      <>
        <Alert variant="success">Journalposten ble flyttet til sak {sakid} og journalføringsoppgave opprettet.</Alert>

        <br />

        <FlexRow justify="right">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>
          {mapSuccess(opprettOppgaveStatus, (oppgave) => (
            <Button onClick={() => navigate(`/oppgave/${oppgave.id}`)}>Gå til oppgaven</Button>
          ))}
        </FlexRow>
      </>
    )
  }

  const isLoading = isPending(oppdaterResult) || isPending(opprettOppgaveStatus) || isPending(tildelSaksbehandlerStatus)

  return mapResult(annenSakStatus, {
    initial: (
      <FlexRow align="end" $spacing>
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
      </FlexRow>
    ),
    pending: <Spinner visible label="Henter sak..." />,
    success: (annenSak) => (
      <>
        {isSuccess(sakStatus) && <SakOverfoeringDetailjer fra={sakStatus.data.sak} til={annenSak} />}
        <br />

        <FlexRow justify="right">
          <Button variant="secondary" onClick={lukkModal} disabled={isLoading}>
            Nei, avbryt
          </Button>
          <Button onClick={() => flyttJournalpost(annenSak)} loading={isLoading}>
            Flytt journalpost til sak {annenSak.id}
          </Button>
        </FlexRow>
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
