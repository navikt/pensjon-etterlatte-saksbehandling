import { BodyShort, Button, Heading, Modal, Select } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

type Props = {
  sakId: number
}
const OpprettRevurderingModal = (props: Props) => {
  const [open, setOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [valgtAarsak, setValgtAarsak] = useState('')
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettRevurderingApi)

  const aapneModal = () => setOpen(true)
  const lukkModal = () => {
    setValgtAarsak('')
    setError(null)
    setOpen(false)
  }

  const onSubmit = () => {
    if (valgtAarsak === '') {
      return setError('Du må velge en årsak')
    }

    opprettRevurdering(
      { sakId: props.sakId, aarsak: valgtAarsak },
      () => {},
      () => {
        setError('En feil skjedde ved opprettelse av revurderingen')
      }
    )
  }

  return (
    <>
      <Button variant="primary" onClick={aapneModal}>
        Opprett revurdering
      </Button>
      <Modal open={open} onClose={lukkModal}>
        <Modal.Content>
          <ModalContentWrapper>
            <Heading spacing size="large">
              Opprett ny revurdering
            </Heading>
            <BodyShort spacing style={{ marginBottom: '2em' }}>
              For å opprette en ny revurdering så må du først velge en årsak for revurderingen
            </BodyShort>
            <div>
              <Select label="Årsak" value={valgtAarsak} onChange={(e) => setValgtAarsak(e.target.value)} error={error}>
                <option value={''}>Velg en årsak</option>
                {Object.values(Revurderingsaarsak).map((aarsak) => (
                  <option value={aarsak} key={aarsak}>
                    {aarsak}
                  </option>
                ))}
              </Select>
            </div>
          </ModalContentWrapper>

          <ButtonContainer>
            <Button loading={isPending(opprettRevurderingStatus)} onClick={onSubmit}>
              Opprett
            </Button>
            <Button onClick={lukkModal}>Avbryt</Button>
          </ButtonContainer>
        </Modal.Content>
      </Modal>
    </>
  )
}

const ModalContentWrapper = styled.div`
  padding: 0 1em;
`

const ButtonContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 0.5em;
  padding-top: 2em;
`

export default OpprettRevurderingModal
