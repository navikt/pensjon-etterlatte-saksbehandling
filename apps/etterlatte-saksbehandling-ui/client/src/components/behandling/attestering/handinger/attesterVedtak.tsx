import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { attesterVedtak } from '~shared/api/behandling'
import { BeslutningWrapper, ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router'
import { behandlingSkalSendeBrev } from '~components/behandling/felles/utils'

export const AttesterVedtak = ({ behandling, kommentar }: { behandling: IDetaljertBehandling; kommentar: string }) => {
  const navigate = useNavigate()
  const [modalisOpen, setModalisOpen] = useState(false)
  const skalSendeBrev = behandlingSkalSendeBrev(behandling)

  const attester = async () => {
    attesterVedtak(behandling.id, kommentar).then((response) => {
      if (response.status === 'ok') {
        navigate(`/person/${behandling.søker?.foedselsnummer}`)
      }
    })
  }

  return (
    <BeslutningWrapper>
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
      />
    </BeslutningWrapper>
  )
}
