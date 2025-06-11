import { BodyShort, Detail } from '@navikt/ds-react'
import { EndringElement, EndringListe } from '~components/behandling/sidemeny/OppgaveEndring'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveKommentarer } from '~shared/api/oppgaver'
import { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'

export const OppgaveKommentarer = ({ oppgaveId }: { oppgaveId: string }) => {
  const [hentOppgaveKommentarerResult, hentOppgaveKommentarerRequest] = useApiCall(hentOppgaveKommentarer)

  useEffect(() => {
    hentOppgaveKommentarerRequest({ oppgaveId })
  }, [])

  return (
    <EndringListe>
      {mapResult(hentOppgaveKommentarerResult, {
        pending: <Spinner label="Henter kommentarer..." />,
        error: <ApiErrorAlert>Kunne ikke hente kommentarer</ApiErrorAlert>,
        success: (kommentarer) =>
          !!kommentarer?.length ? (
            <>
              {kommentarer.map((kommentar, index) => (
                <EndringElement key={index}>
                  <BodyShort size="small">{kommentar.kommentar}</BodyShort>
                  <Detail>utført av {kommentar.saksbehandler.navn}</Detail>
                  <Detail>{formaterDatoMedKlokkeslett(kommentar.tidspunkt)}</Detail>
                </EndringElement>
              ))}
            </>
          ) : (
            <BodyShort>Ingen historikk</BodyShort>
          ),
      })}
    </EndringListe>
  )
}
