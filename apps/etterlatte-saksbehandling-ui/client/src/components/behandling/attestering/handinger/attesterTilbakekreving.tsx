import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { attesterVedtak } from '~shared/api/tilbakekreving'

export const AttesterTilbakekreving = ({
  tilbakekreving,
  kommentar,
}: {
  tilbakekreving: TilbakekrevingBehandling
  kommentar: string
}) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)
  const [error, setError] = useState<string>()

  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtak)

  const attester = () => {
    // TODO EY-2806 Ferdigstill brev?
    apiAttesterVedtak(
      { behandlingsId: tilbakekreving.id, kommentar },
      () => navigate(`/person/${tilbakekreving.sak.ident}`),
      () => {
        setError(`Ukjent feil oppsto ved attestering av vedtaket... Prøv igjen.`)
        setModalisOpen(false)
      }
    )
  }

  return (
    <BeslutningWrapper>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}
      <FlexRow>
        <Button variant="primary" onClick={() => setModalisOpen(true)}>
          Iverksett vedtak
        </Button>
      </FlexRow>
      <GeneriskModal
        tittel="Er du sikker på at du vil attestere vedtaket?"
        beskrivelse="FYLL INN BESKRIVELSE"
        tekstKnappJa="Ja, attester vedtak"
        tekstKnappNei="Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(attesterVedtakStatus)}
      />
    </BeslutningWrapper>
  )
}
