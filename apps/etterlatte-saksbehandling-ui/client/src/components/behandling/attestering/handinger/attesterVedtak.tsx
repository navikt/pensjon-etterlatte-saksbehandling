import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak } from '~shared/api/behandling'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { attesterVedtaksbrev } from '~shared/api/brev'
import { useAppSelector } from '~store/Store'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export const AttesterVedtak = ({ behandlingId }: { behandlingId?: string }) => {
  const { behandlingType } = useAppSelector((state) => state.behandlingReducer.behandling)

  const [modalisOpen, setModalisOpen] = useState(false)

  const ferdigstillVedtaksbrev = async () => {
    if (behandlingType === IBehandlingsType.MANUELT_OPPHOER) {
      return true
    }

    return await attesterVedtaksbrev(behandlingId!!).then((response) => {
      if (response.status === 'ok') return true
      else throw new Error(`Feil oppsto ved attestering av brev: \n${response.error}`)
    })
  }

  const attester = async () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    const vedtaksbrevAttestertOK = await ferdigstillVedtaksbrev()

    if (vedtaksbrevAttestertOK) {
      attesterVedtak(behandlingId).then((response) => {
        if (response.status === 'ok') {
          window.location.reload()
        }
      })
    }
  }

  return (
    <>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Iverksett vedtak
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tekst="Er du sikker på at vil iverksette vedtaket?"
        tekstKnappJa="Ja, iverksett vedtak"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={attester}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
      />
    </>
  )
}
