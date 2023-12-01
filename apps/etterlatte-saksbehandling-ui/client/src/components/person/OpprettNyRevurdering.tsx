import { Button, Heading, Modal, Select, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { useNavigate } from 'react-router-dom'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'

import { isPending } from '~shared/api/apiUtils'

export const OpprettNyRevurdering = ({
  sakId,
  revurderinger,
}: {
  sakId: number
  revurderinger: Array<Revurderingaarsak>
}) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [valgtRevurdering, setValgtRevurdering] = useState<Revurderingaarsak | undefined>()
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)
  const navigate = useNavigate()
  const opprettBehandling = () => {
    if (valgtRevurdering === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      { sakId: sakId, aarsak: valgtRevurdering, fritekstAarsak: fritekstgrunn ? fritekstgrunn : null },
      (behandlingId: string) => {
        navigate(`/behandling/${behandlingId}/`)
      },
      (err) => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
        console.error(`Feil ved opprettelse av revurdering, status: ${err.status} err: ${err.detail}`)
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
              onChange={(e) => setValgtRevurdering(e.target.value as Revurderingaarsak)}
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
            {valgtRevurdering === Revurderingaarsak.ANNEN && (
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
            <ButtonGroup>
              <Button variant="secondary" onClick={closeAndReset}>
                Avbryt
              </Button>
              <Button loading={isPending(opprettRevurderingStatus)} onClick={opprettBehandling}>
                Opprett
              </Button>
            </ButtonGroup>
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
