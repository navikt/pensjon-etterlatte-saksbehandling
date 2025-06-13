import { BodyShort, Detail } from '@navikt/ds-react'
import { EndringElement, EndringListe } from '~components/behandling/sidemeny/OppgaveEndring'
import { mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { OppgaveKommentar } from '~shared/types/oppgave'

export const OppgaveKommentarer = ({
  hentOppgaveKommentarerResult,
}: {
  hentOppgaveKommentarerResult: Result<Array<OppgaveKommentar>>
}) => {
  // TODO: sortere på tidspunkt, og ikke bare reverser lista
  return mapResult(hentOppgaveKommentarerResult, {
    pending: <Spinner label="Henter kommentarer..." />,
    error: <ApiErrorAlert>Kunne ikke hente kommentarer</ApiErrorAlert>,
    success: (kommentarer) =>
      !!kommentarer?.length ? (
        <EndringListe>
          {kommentarer.toReversed().map((kommentar, index) => (
            <EndringElement key={index}>
              <BodyShort size="small">{kommentar.kommentar}</BodyShort>
              <Detail>utført av {kommentar.saksbehandler.ident}</Detail>
              <Detail>{formaterDatoMedKlokkeslett(kommentar.tidspunkt)}</Detail>
            </EndringElement>
          ))}
        </EndringListe>
      ) : (
        <BodyShort>Ingen historikk</BodyShort>
      ),
  })
}
