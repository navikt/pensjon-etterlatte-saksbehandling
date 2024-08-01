import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GeneriskModal } from '~shared/modal/modal'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router'
import { ferdigstillVedtaksbrev } from '~shared/api/brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { attesterVedtak } from '~shared/api/vedtaksvurdering'

import { isPending } from '~shared/api/apiUtils'
import { hentSakId } from '~components/person/sakOgBehandling/sakUtils'

export const AttesterYtelse = ({ behandling, kommentar }: { behandling: IDetaljertBehandling; kommentar: string }) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)
  const skalSendeBrev = behandling.sendeBrev
  const [error, setError] = useState<string>()
  const [ferdigstillVedtaksbrevStatus, apiFerdigstillVedtaksbrev] = useApiCall(ferdigstillVedtaksbrev)
  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtak)
  const sakId = hentSakId()

  const settVedtakTilAttestert = () => {
    apiAttesterVedtak(
      { behandlingId: behandling.id, kommentar },
      () => navigate(`/sak/${sakId}`),
      (error) => {
        if (error.code === 'ATTESTANT_OG_SAKSBEHANDLER_ER_SAMME_PERSON') {
          setError('Vedtaket er allerede fattet av deg. Du kan ikke attestere dine egne vedtak.')
        } else {
          setError(`En feil opptod ved attestering av vedtaket: ${error.detail}.`)
        }
        setModalisOpen(false)
      }
    )
  }

  const attester = () => {
    if (!skalSendeBrev) {
      settVedtakTilAttestert()
    } else {
      apiFerdigstillVedtaksbrev(
        behandling.id,
        () => settVedtakTilAttestert(),
        () => {
          setError(`Feil oppsto ved ferdigstilling av vedtaksbrevet... Prøv igjen.`)
          setModalisOpen(false)
        }
      )
    }
  }

  return (
    <>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}

      <Button variant="primary" onClick={() => setModalisOpen(true)}>
        Iverksett vedtak {skalSendeBrev ? 'og send brev' : ''}
      </Button>

      <GeneriskModal
        tittel="Er du sikker på at du vil iverksette vedtaket?"
        beskrivelse={
          skalSendeBrev ? 'Vedtaksbrevet sendes ut automatisk ved iverksettelse' : 'Det sendes ikke ut vedtaksbrev'
        }
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei="Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(attesterVedtakStatus) || isPending(ferdigstillVedtaksbrevStatus)}
      />
    </>
  )
}
