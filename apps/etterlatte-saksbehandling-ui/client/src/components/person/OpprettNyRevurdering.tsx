import { Button, Heading, Modal, Select, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'
import { Revurderingsaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { useNavigate } from 'react-router-dom'
import { ButtonContainer } from '~components/person/VurderHendelseModal'

export const OpprettNyRevurdering = ({
  sakId,
  revurderinger,
}: {
  sakId: number
  revurderinger: Array<Revurderingsaarsak>
}) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [valgtRevurdering, setValgtRevurdering] = useState<Revurderingsaarsak | undefined>()
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)
  const navigate = useNavigate()
  const opprettBehandling = () => {
    if (valgtRevurdering === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      { sakId: sakId, aarsak: valgtRevurdering, fritekstAarsak: fritekstgrunn },
      (behandlingId: string) => {
        navigate(`/behandling/${behandlingId}/`)
      },
      (err) => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
        console.error(`Feil ved opprettelse av revurdering, status: ${err.status} err: ${err.error}`)
      }
    )
  }

  const closeAndReset = () => {
    setOpen(false)
    resetApiCall()
  }

  return (
    <OpprettRevurderingWrapper>
      <>
        <Button variant="secondary" onClick={() => setOpen(true)}>
          Opprett ny revurdering
        </Button>
        <Modal open={open} onClose={closeAndReset} aria-labelledby="modal-heading">
          <Modal.Body>
            <Modal.Header closeButton={false}>
              <Heading spacing level="2" size="medium" id="modal-heading">
                Opprett ny revurdering
              </Heading>
            </Modal.Header>
            <Select
              label="Årsak til revurdering"
              value={valgtRevurdering}
              onChange={(e) => setValgtRevurdering(e.target.value as Revurderingsaarsak)}
              error={error}
            >
              <option>Velg type</option>
              {revurderinger.map((revurdering, i) => {
                return (
                  <option key={`revurdering${i}`} value={revurdering}>
                    {tekstRevurderingsaarsak[revurdering]}
                  </option>
                )
              })}
            </Select>
            {valgtRevurdering === Revurderingsaarsak.ANNEN && (
              <AnnenRevurderingWrapper>
                <TextField
                  label="Beskriv årsak"
                  size="medium"
                  type="text"
                  value={fritekstgrunn}
                  onChange={(e) => setFritekstgrunn(e.target.value)}
                />
              </AnnenRevurderingWrapper>
            )}
            <ButtonContainer>
              <Button variant="secondary" onClick={closeAndReset}>
                Avbryt
              </Button>
              <Button loading={isPending(opprettRevurderingStatus)} onClick={opprettBehandling}>
                Opprett
              </Button>
            </ButtonContainer>
          </Modal.Body>
        </Modal>
      </>
    </OpprettRevurderingWrapper>
  )
}

const OpprettRevurderingWrapper = styled.div`
  margin-top: 3rem;
`

const AnnenRevurderingWrapper = styled.div`
  margin-top: 1rem;
`
