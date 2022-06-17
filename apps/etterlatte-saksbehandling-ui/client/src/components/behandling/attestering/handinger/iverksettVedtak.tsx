import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { iverksettVedtak } from '../../../../shared/api/behandling'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '../modal'

export const IverksettVedtak = ({ behandlingId }: { behandlingId?: string }) => {
  const [modalisOpen, setModalisOpen] = useState(false)

  const iverksett = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    const result = iverksettVedtak(behandlingId).then((response) => {
      console.log(response)
      if (response.status === 200) {
        window.location.reload()
      }
    })
    console.log(result)
  }

  return (
    <>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Iverksett vedtak
        </Button>
      </ButtonWrapper>
      {modalisOpen && (
        <GeneriskModal
          tekst="Er du sikker på at vil iverksette vedtaket?"
          tekstKnappJa="Ja, iverksett vedtak"
          tekstKnappNei=" Nei, gå tilbake"
          funksjon={iverksett}
          setModalisOpen={setModalisOpen}
        />
      )}
    </>
  )
}
