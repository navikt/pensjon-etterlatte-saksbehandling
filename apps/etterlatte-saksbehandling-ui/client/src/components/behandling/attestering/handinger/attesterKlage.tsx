import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { attesterVedtakOmAvvistKlage } from '~shared/api/klage'

import { isPending } from '~shared/api/apiUtils'
import { Klage } from '~shared/types/Klage'

export const AttesterKlage = ({ klage, kommentar }: { klage: Klage; kommentar: string }) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)
  const [error, setError] = useState<string>()

  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtakOmAvvistKlage)

  const attester = () => {
    apiAttesterVedtak(
      { klageId: klage.id, kommentar: kommentar },
      () => navigate(`/person/${klage.sak.id}`),
      () => {
        setError(`Ukjent feil oppsto ved attestering av vedtaket... Prøv igjen.`)
        setModalisOpen(false)
      }
    )
  }

  return (
    <div>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}
      <Button variant="primary" onClick={() => setModalisOpen(true)}>
        Attester vedtak
      </Button>
      <GeneriskModal
        tittel="Er du sikker på at du vil attestere vedtaket?"
        beskrivelse="Vedtaksbrevet sendes ut automatisk"
        tekstKnappJa="Ja, attester vedtak"
        tekstKnappNei="Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(attesterVedtakStatus)}
      />
    </div>
  )
}
