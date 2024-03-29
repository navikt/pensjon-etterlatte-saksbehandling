import React, { useState } from 'react'
import { Alert, Button, TextField } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { Journalpost, Journalstatus, Sakstype } from '~shared/types/Journalpost'
import { ISak } from '~shared/types/sak'
import { hentSak } from '~shared/api/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { SakMedBehandlinger } from '~components/person/typer'
import { FlexRow } from '~shared/styled'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { feilregistrerSakstilknytning, knyttTilAnnenSak } from '~shared/api/dokument'
import { MagnifyingGlassIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { useNavigate } from 'react-router-dom'
import { SakOverfoeringDetailjer } from 'src/components/person/dokumenter/avvik/common/SakOverfoeringDetailjer'

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

  const [sakid, setSakid] = useState<string>()

  const [annenSakStatus, hentAnnenSak] = useApiCall(hentSak)
  const [knyttTilAnnenSakStatus, apiKnyttTilAnnenSak] = useApiCall(knyttTilAnnenSak)
  const [feilregSakstilknytningStatus, apiFeilregistrerSakstilknytning] = useApiCall(feilregistrerSakstilknytning)

  const flyttJournalpost = (sak: ISak) => {
    apiKnyttTilAnnenSak({
      journalpostId: journalpost.journalpostId,
      request: {
        bruker: {
          id: sak.ident,
          idType: 'FNR',
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
    if (journalpost.journalstatus === Journalstatus.FEILREGISTRERT) {
      flyttJournalpost(sak)
    } else {
      apiFeilregistrerSakstilknytning(journalpost.journalpostId, () => {
        flyttJournalpost(sak)
      })
    }
  }

  if (isSuccess(knyttTilAnnenSakStatus) && isSuccess(feilregSakstilknytningStatus)) {
    return (
      <>
        <Alert variant="success">
          Journalposten ble feilregistrert og flyttet til annen sak{' '}
          {mapSuccess(annenSakStatus, (sak) => `(sakid=${sak.id}, fnr=${sak.ident}, saktype=${sak.sakType})`)}
        </Alert>

        <br />

        <FlexRow justify="right">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>
          {mapSuccess(annenSakStatus, (sak) => (
            <Button onClick={() => navigate(`/person/${sak.ident}`)}>Gå til sak {sak.id}</Button>
          ))}
        </FlexRow>
      </>
    )
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

            <Alert variant="warning" inline>
              OBS! Journalposten du flytter vil automatisk bli markert som feilregistrert, slik at kun den nye blir
              gjeldende.
            </Alert>
            <br />

            {mapFailure(knyttTilAnnenSakStatus, (error) => (
              <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved flytting av journalpost'}</Alert>
            ))}

            {mapFailure(feilregSakstilknytningStatus, (error) => (
              <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved feilregistrering av journalpost'}</Alert>
            ))}

            <FlexRow justify="right">
              <Button variant="secondary" onClick={lukkModal}>
                Nei, avbryt
              </Button>
              <Button
                onClick={() => flyttOgFeilregistrerJournalpost(annenSak)}
                loading={isPending(feilregSakstilknytningStatus) || isPending(knyttTilAnnenSakStatus)}
              >
                Flytt journalpost til sak {annenSak.id}
              </Button>
            </FlexRow>
          </>
        ),
        error: (error) => (
          <>
            {error.status === 404 ? (
              <Alert variant="warning">Fant ikke sak {sakid}</Alert>
            ) : (
              <Alert variant="error">Ukjent feil ved henting av sak {sakid}</Alert>
            )}
          </>
        ),
      })}
    </>
  )
}
