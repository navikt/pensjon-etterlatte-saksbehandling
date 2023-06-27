import { Button, Heading, Modal, Select } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'
import { Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'
import { ButtonContainer } from '~components/person/OpprettRevurderingModal'

export const OpprettNyBehandling = ({
  sakId,
  revurderinger,
}: {
  sakId: number
  revurderinger: Array<Revurderingsaarsak>
}) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [valgtRevurdering, setValgtRevurdering] = useState<Revurderingsaarsak | undefined>()
  const stoettedeRevurderinger = revurderinger

  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)
  const navigate = useNavigate()
  const opprettBehandling = () => {
    if (valgtRevurdering === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      { sakId: sakId, aarsak: valgtRevurdering },
      (behandling: IDetaljertBehandling) => {
        navigate(`/behandling/${behandling.id}/`)
      },
      (err) => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
        console.error(`Feil ved opprettelse av revurdering, status: ${err.status} err: ${err.error}`)
      }
    )
  }

  return (
    <OpprettNyBehandlingWrapper>
      <>
        <Button variant="secondary" onClick={() => setOpen(true)}>
          Opprett ny behandling
        </Button>
        <Modal open={open} onClose={() => setOpen((x) => !x)} closeButton={false} aria-labelledby="modal-heading">
          <Modal.Content>
            <Heading spacing level="2" size="medium" id="modal-heading">
              Opprett ny behandling
            </Heading>
            <Select
              label="Årsak til revurdering"
              value={valgtRevurdering}
              onChange={(e) => setValgtRevurdering(e.target.value as Revurderingsaarsak)}
              error={error}
            >
              <option>Velg type</option>
              {stoettedeRevurderinger.map((revurdering, i) => {
                return (
                  <option key={`revurdering${i}`} value={revurdering}>
                    {tekstRevurderingsaarsak[revurdering]}
                  </option>
                )
              })}
            </Select>
            <ButtonContainer>
              <Button
                variant="secondary"
                onClick={() => {
                  setOpen(false)
                  resetApiCall()
                }}
              >
                Avbryt
              </Button>
              <Button loading={isPending(opprettRevurderingStatus)} onClick={opprettBehandling}>
                Opprett
              </Button>
            </ButtonContainer>
          </Modal.Content>
        </Modal>
      </>
    </OpprettNyBehandlingWrapper>
  )
}

const OpprettNyBehandlingWrapper = styled.div`
  margin-top: 3rem;
`
