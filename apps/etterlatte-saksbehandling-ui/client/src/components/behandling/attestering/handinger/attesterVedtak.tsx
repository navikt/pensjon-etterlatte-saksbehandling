import { Alert, Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak } from '~shared/api/behandling'
import { BeslutningWrapper, ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router'
import { behandlingSkalSendeBrev } from '~components/behandling/felles/utils'
import { hentVedtaksbrev } from '~shared/api/brev'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { BrevStatus } from '~shared/types/Brev'

export const AttesterVedtak = ({ behandling, kommentar }: { behandling: IDetaljertBehandling; kommentar: string }) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)
  const skalSendeBrev = behandlingSkalSendeBrev(behandling)
  const [error, setError] = useState<string>()
  const [hentVedtaksbrevStatus, apiHentVedtaksbrev] = useApiCall(hentVedtaksbrev)
  const [attesterVedtakStatus, apiAttesterVedtak] = useApiCall(attesterVedtak)

  const settVedtakTilAttestert = () => {
    apiAttesterVedtak(
      { behandlingId: behandling.id, kommentar },
      () => navigate(`/person/${behandling.søker?.foedselsnummer}`),
      () => {
        setError(`Ukjent feil oppsto ved attestering av vedtaket... Prøv igjen.`)
        setModalisOpen(false)
      }
    )
  }

  const attester = () => {
    if (!skalSendeBrev) {
      settVedtakTilAttestert()
    } else {
      apiHentVedtaksbrev(behandling.id, (vedtaksbrev) => {
        if (vedtaksbrev.status === BrevStatus.FERDIGSTILT) {
          settVedtakTilAttestert()
        } else {
          setError(`Brev har feil status (${vedtaksbrev.status.toLowerCase()}).
                    Kan ikke attestere saken. Forsøk å laste siden på nytt.`)
          setModalisOpen(false)
        }
      })
    }
  }

  return (
    <BeslutningWrapper>
      {error && (
        <Alert variant={'error'} style={{ marginTop: '1rem' }}>
          {error}
        </Alert>
      )}
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          {`Iverksett vedtak ${skalSendeBrev ? 'og send brev' : ''}`}
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tittel="Er du sikker på at du vil iverksette vedtaket?"
        beskrivelse="Vedtaksbrevet sendes ut automatisk ved iverksettelse"
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
        loading={isPending(attesterVedtakStatus) || isPending(hentVedtaksbrevStatus)}
      />
    </BeslutningWrapper>
  )
}
