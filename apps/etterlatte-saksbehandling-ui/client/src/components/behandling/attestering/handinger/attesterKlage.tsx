import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
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
      () => navigate(`/person/${klage.sak.ident}`),
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
        beskrivelse="Vedtaksbrevet sendes ut automatisk ved iverksettelse"
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
