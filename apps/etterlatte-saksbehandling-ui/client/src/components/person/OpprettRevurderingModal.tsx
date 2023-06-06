import { BodyShort, Button, Heading, Modal, Select } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'

type Props = {
  open: boolean
  setOpen: (value: boolean) => void
  sakId: number
  revurderinger: Array<string>
  valgtHendelse: Grunnlagsendringshendelse
}
const OpprettRevurderingModal = (props: Props) => {
  const { revurderinger } = props
  const [error, setError] = useState<string | null>(null)
  const [valgtAarsak, setValgtAarsak] = useState<Revurderingsaarsak | undefined>(undefined)
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettRevurderingApi)
  const navigate = useNavigate()

  const onSubmit = () => {
    if (valgtAarsak === undefined) {
      return setError('Du må velge en årsak')
    }

    opprettRevurdering(
      { sakId: props.sakId, aarsak: valgtAarsak },
      (revurdering: IDetaljertBehandling) => {
        navigate(`/behandling/${revurdering.id}/`)
      },
      () => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
      }
    )
  }

  return (
    <>
      <Modal open={props.open} onClose={() => props.setOpen(false)}>
        <Modal.Content>
          <ModalContentWrapper>
            <Heading spacing size="large">
              Opprett ny revurdering
            </Heading>
            <BodyShort spacing style={{ marginBottom: '2em' }}>
              For å opprette en ny revurdering så må du først velge en årsak for revurderingen
            </BodyShort>
            <div>
              <Select
                label="Årsak"
                value={valgtAarsak}
                onChange={(e) => setValgtAarsak(e.target.value as Revurderingsaarsak)}
                error={error}
              >
                <option value={''}>Velg en årsak</option>
                {revurderinger.map((aarsak) => (
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
            <Button onClick={() => props.setOpen(false)}>Avbryt</Button>
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
