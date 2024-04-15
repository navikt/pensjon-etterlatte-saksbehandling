import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router'
import { ferdigstillVedtaksbrev } from '~shared/api/brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { FlexRow } from '~shared/styled'
import { attesterVedtak } from '~shared/api/vedtaksvurdering'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

import { isPending } from '~shared/api/apiUtils'

export const AttesterYtelse = ({ behandling, kommentar }: { behandling: IDetaljertBehandling; kommentar: string }) => {
  const navigate = useNavigate()
  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const [modalisOpen, setModalisOpen] = useState(false)
  const skalSendeBrev = behandling.sendeBrev
  const [error, setError] = useState<string>()
  const [ferdigstillVedtaksbrevStatus, apiFerdigstillVedtaksbrev] = useApiCall(ferdigstillVedtaksbrev)
  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtak)

  const settVedtakTilAttestert = () => {
    apiAttesterVedtak(
      { behandlingId: behandling.id, kommentar },
      () => navigate(`/person/${soeker?.foedselsnummer}`),
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
    <BeslutningWrapper>
      {error && (
        <Alert variant="error" style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}
      <FlexRow>
        <Button variant="primary" onClick={() => setModalisOpen(true)}>
          Iverksett vedtak {skalSendeBrev ? 'og send brev' : ''}
        </Button>
      </FlexRow>
      <GeneriskModal
        tittel="Er du sikker på at du vil iverksette vedtaket?"
        beskrivelse="Vedtaksbrevet sendes ut automatisk ved iverksettelse"
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei="Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(attesterVedtakStatus) || isPending(ferdigstillVedtaksbrevStatus)}
      />
    </BeslutningWrapper>
  )
}
