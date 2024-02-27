import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { useNavigate } from 'react-router'
import { ferdigstillVedtaksbrev } from '~shared/api/brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { attesterVedtak } from '~shared/api/vedtaksvurdering'

import { isPending } from '~shared/api/apiUtils'

export const AttesterVedtak = ({
  behandlingId,
  soekerIdent,
  skalSendeBrev,
  kommentar,
  skalFerdigstilleBrev,
}: {
  behandlingId: string
  soekerIdent: string
  skalSendeBrev: boolean
  skalFerdigstilleBrev: boolean
  kommentar: string
}) => {
  const navigate = useNavigate()
  const [modalIsOpen, setModalIsOpen] = useState(false)
  const [error, setError] = useState<string>()
  const [ferdigstillVedtaksbrevStatus, apiFerdigstillVedtaksbrev] = useApiCall(ferdigstillVedtaksbrev)
  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtak)

  const settVedtakTilAttestert = () => {
    apiAttesterVedtak(
      { behandlingId: behandlingId, kommentar },
      () => navigate(`/person/${soekerIdent}`),
      (error) => {
        setError(
          error.code === 'ATTESTANT_OG_SAKSBEHANDLER_ER_SAMME_PERSON'
            ? 'Vedtaket er allerede fattet av deg. Du kan ikke attestere dine egne vedtak.'
            : `En feil opptod ved attestering av vedtaket: ${error.detail}.`
        )
        setModalIsOpen(false)
      }
    )
  }

  const attester = () => {
    if (!skalFerdigstilleBrev) {
      settVedtakTilAttestert()
    } else {
      apiFerdigstillVedtaksbrev(
        behandlingId,
        () => settVedtakTilAttestert(),
        () => {
          setError(`Feil oppsto ved ferdigstilling av vedtaksbrevet... Prøv igjen.`)
          setModalIsOpen(false)
        }
      )
    }
  }

  return (
    <BeslutningWrapper>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}
      <FlexRow>
        <Button variant="primary" onClick={() => setModalIsOpen(true)}>
          Iverksett vedtak {skalSendeBrev ? 'og send brev' : ''}
        </Button>
      </FlexRow>
      <GeneriskModal
        tittel="Er du sikker på at du vil iverksette vedtaket?"
        beskrivelse={skalSendeBrev ? 'Vedtaksbrevet sendes ut automatisk ved iverksettelse' : ''}
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei="Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalIsOpen}
        open={modalIsOpen}
        loading={isPending(attesterVedtakStatus) || isPending(ferdigstillVedtaksbrevStatus)}
      />
    </BeslutningWrapper>
  )
}
