import React, { useState } from 'react'
import { Alert, Button, HStack, Radio, RadioGroup, TextField } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { BrukerIdType, Journalpost, Journalstatus, Sakstype } from '~shared/types/Journalpost'
import { ISak } from '~shared/types/sak'
import { hentSak } from '~shared/api/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakMedBehandlinger } from '~components/person/typer'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { feilregistrerSakstilknytning, knyttTilAnnenSak } from '~shared/api/dokument'
import { MagnifyingGlassIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { useNavigate } from 'react-router-dom'
import { SakOverfoeringDetailjer } from 'src/components/person/dokumenter/avvik/common/SakOverfoeringDetailjer'
import { opprettOppgave } from '~shared/api/oppgaver'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { JaNei } from '~shared/types/ISvar'
import { formaterSakstype } from '~utils/formatering/formatering'
import { ClickEvent, trackClick } from '~utils/analytics'
import { ApiError } from '~shared/api/apiClient'

const erSammeSak = (sak: ISak, journalpost: Journalpost): boolean => {
  const { sak: journalpostSak, tema } = journalpost

  return (
    !!journalpostSak &&
    journalpostSak.fagsakId === sak.id.toString() &&
    journalpostSak.sakstype === Sakstype.FAGSAK &&
    journalpostSak.fagsaksystem === 'EY' &&
    tema === temaFraSakstype(sak.sakType)
  )
}

/**
 * Knytt til annen sak brukes i tilfeller hvor journalposten er JOURNALFOERT / FERDIGSTILT.
 * Når en journalpost er i denne tilstanden kan den ikke redigeres på vanlig vis og må dermed
 * feilregistreres og flyttes til ny sak (dokarkiv lager bare kopi av den gamle).
 **/
export const KnyttTilAnnenSak = ({
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
  const [skalFeilregistrere, setSkalFeilregistrere] = useState<JaNei>()

  const [annenSakStatus, hentAnnenSak] = useApiCall(hentSak)
  const [knyttTilAnnenSakStatus, apiKnyttTilAnnenSak] = useApiCall(knyttTilAnnenSak)
  const [feilregSakstilknytningStatus, apiFeilregistrerSakstilknytning] = useApiCall(feilregistrerSakstilknytning)

  const [oppgaveResult, apiOpprettOppgave] = useApiCall(opprettOppgave)

  const flyttJournalpost = (sak: ISak) => {
    trackClick(ClickEvent.KNYTT_JOURNALPOST_TIL_ANNEN_SAK)

    apiKnyttTilAnnenSak({
      journalpostId: journalpost.journalpostId,
      request: {
        bruker: {
          id: sak.ident,
          idType: BrukerIdType.FNR,
        },
        fagsakId: sak.id.toString(),
        fagsaksystem: 'EY',
        journalfoerendeEnhet: sak.enhet,
        sakstype: 'FAGSAK',
        tema: temaFraSakstype(sak.sakType),
      },
    })
  }

  const flyttOgFeilregistrerJournalpost = (sak: ISak) => {
    if (skalFeilregistrere === JaNei.JA && journalpost.journalstatus !== Journalstatus.FEILREGISTRERT) {
      apiFeilregistrerSakstilknytning(journalpost.journalpostId, () => {
        flyttJournalpost(sak)
      })
    } else {
      flyttJournalpost(sak)
    }
  }

  const opprettJournalfoeringsoppgave = (sakId: number, journalpostId: string) =>
    apiOpprettOppgave(
      {
        sakId,
        request: {
          oppgaveType: Oppgavetype.JOURNALFOERING,
          referanse: journalpostId,
          merknad: `Journalpost flyttet til sak ${sakId}`,
          oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
          saksbehandler: innloggetSaksbehandler.ident,
        },
      },
      (oppgave) => {
        navigate(`/oppgave/${oppgave.id}`)
      }
    )

  if (
    isSuccess(knyttTilAnnenSakStatus) &&
    (isSuccess(feilregSakstilknytningStatus) || skalFeilregistrere === JaNei.NEI)
  ) {
    return mapSuccess(annenSakStatus, (sak) => (
      <>
        {skalFeilregistrere === JaNei.JA ? (
          <Alert variant="success">
            Journalposten ble feilregistrert og flyttet til sak {sak.id} ({formaterSakstype(sak.sakType)})
          </Alert>
        ) : (
          <Alert variant="success">
            Journalposten ble knyttet til sak {sak.id} ({formaterSakstype(sak.sakType)})
          </Alert>
        )}

        <br />

        <Alert variant="info" inline>
          Om du oppretter oppgave vil den bli knyttet til den <i>nye</i> saken og journalposten
        </Alert>

        <br />

        <HStack gap="space-4" justify="end">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>

          <Button variant="secondary" onClick={() => navigate('/person', { state: { fnr: sak.ident } })}>
            Gå til sak {sak.id}
          </Button>
          <Button
            onClick={() => opprettJournalfoeringsoppgave(sak.id, knyttTilAnnenSakStatus.data.nyJournalpostId)}
            loading={isPending(oppgaveResult)}
          >
            Opprett oppgave
          </Button>
        </HStack>
      </>
    ))
  }
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
  return (
    <>
      {mapResult(sakStatus, {
        success: ({ sak }) =>
          erSammeSak(sak, journalpost) ? (
            <Alert variant="info" inline>
              Journalposten er allerede tilknyttet brukerens sak i Gjenny. <br />
              Ønsker du å flytte journalposten til en annen bruker sin sak?
            </Alert>
          ) : (
            <Alert variant="warning">Journalposten er ikke tilknyttet denne brukeren sin sak.</Alert>
          ),
        error: (error) => (
          <>
            {' '}
            TODO sjekke mot 401
            {error.status === 404 ? (
              <Alert variant="warning">Brukeren har ingen sak i Gjenny</Alert>
            ) : (
              <Alert variant="error">Ukjent feil ved henting av brukerens sak</Alert>
            )}
          </>
        ),
      })}

      <br />

      {mapResult(annenSakStatus, {
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
          <>
            {mapSuccess(sakStatus, (data) => (
              <SakOverfoeringDetailjer fra={data.sak} til={annenSak} />
            ))}
            <br />

            <RadioGroup
              legend="Feilregistrer den gamle journalposten?"
              description='Hvis "Ja" vil journalposten du flytter vil automatisk bli markert som feilregistrert, slik at kun den nye blir gjeldende.'
              onChange={(verdi) => setSkalFeilregistrere(verdi)}
            >
              <Radio value={JaNei.JA}>Ja, feilregistrer</Radio>
              <Radio value={JaNei.NEI}>Nei, behold begge</Radio>
            </RadioGroup>

            <br />

            {mapFailure(knyttTilAnnenSakStatus, (error) => (
              <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved flytting av journalpost'}</Alert>
            ))}

            {mapFailure(feilregSakstilknytningStatus, (error) => (
              <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved feilregistrering av journalpost'}</Alert>
            ))}

            <HStack gap="space-4" justify="end">
              <Button variant="secondary" onClick={lukkModal}>
                Nei, avbryt
              </Button>
              <Button
                onClick={() => flyttOgFeilregistrerJournalpost(annenSak)}
                loading={isPending(feilregSakstilknytningStatus) || isPending(knyttTilAnnenSakStatus)}
                disabled={skalFeilregistrere === undefined}
              >
                Flytt journalpost til sak {annenSak.id}
              </Button>
            </HStack>
          </>
        ),
        error: (error) => feilkodehaandtering(error),
      })}
    </>
  )
}
