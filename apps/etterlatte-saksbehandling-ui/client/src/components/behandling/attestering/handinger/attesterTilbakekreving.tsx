import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { attesterVedtak } from '~shared/api/tilbakekreving'

import { isPending } from '~shared/api/apiUtils'

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
    apiAttesterVedtak(
      { behandlingsId: tilbakekreving.id, kommentar },
      () => navigate(`/person/${tilbakekreving.sak.ident}`),
      (error) => {
        setError(`Ukjent feil oppsto ved attestering av vedtaket: ${error.detail}`)
        setModalisOpen(false)
      }
    )
  }

  return (
    <BeslutningWrapper>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem', marginBottom: '1rem' }}>
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
        beskrivelse="Vedtaket vil sendes økonomi og brev sendes til bruker."
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
