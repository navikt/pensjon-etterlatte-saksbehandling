import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak } from '~shared/api/behandling'
import { BeslutningWrapper, ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { attesterVedtaksbrev } from '~shared/api/brev'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router'

export const AttesterVedtak = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)

  const ferdigstillVedtaksbrev = async () => {
    if (behandling.behandlingType === IBehandlingsType.MANUELT_OPPHOER) {
      return true
    }

    return attesterVedtaksbrev(behandling.id).then((response) => {
      if (response.status === 'ok') return true
      else throw new Error(`Feil oppsto ved attestering av brev: \n${response.error}`)
    })
  }

  const attester = async () => {
    const vedtaksbrevAttestertOK = await ferdigstillVedtaksbrev()

    if (vedtaksbrevAttestertOK) {
      attesterVedtak(behandling.id).then((response) => {
        if (response.status === 'ok') {
          navigate(`/person/${behandling.søker?.foedselsnummer}`)
        }
      })
    }
  }

  return (
    <BeslutningWrapper>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Iverksett vedtak
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tekst="Er du sikker på at du vil iverksette vedtaket?"
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
      />
    </BeslutningWrapper>
  )
}
