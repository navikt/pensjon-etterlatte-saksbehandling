import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { ButtonWrapper } from '../styled'
import { GeneriskModal } from '~shared/modal/modal'
import { hentBehandling, underkjennVedtak } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

type Props = {
  behandling: IDetaljertBehandling
  kommentar: string
  valgtBegrunnelse: string
}

export const UnderkjennVedtak: React.FC<Props> = ({ behandling, kommentar, valgtBegrunnelse }) => {
  const [modalisOpen, setModalisOpen] = useState(false)
  const navigate = useNavigate()

  const underkjenn = () => {
    if (!behandling.id) throw new Error('Mangler behandlingsid')

    underkjennVedtak(behandling.id, kommentar, valgtBegrunnelse).then((response) => {
      if (response.status === 'ok') {
        hentBehandling(behandling.id).then((response) => {
          if (response.status === 'ok') {
            navigate(`/person/${behandling.søker?.foedselsnummer}`)
          }
        })
      }
    })
  }

  return (
    <>
      <ButtonWrapper>
        <Button variant="primary" size="medium" className="button" onClick={() => setModalisOpen(true)}>
          Bekreft og send i retur
        </Button>
      </ButtonWrapper>
      <GeneriskModal
        tekst="Er du sikker på at vil underkjenne vedtak og sende i retur til saksbehandler?"
        tekstKnappJa="Ja, send i retur"
        tekstKnappNei=" Nei, gå tilbake"
        onYesClick={underkjenn}
        setModalisOpen={setModalisOpen}
        open={modalisOpen}
      />
    </>
  )
}
