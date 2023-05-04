import { Button, Heading, Modal, Select } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { stoettedeRevurderingerSomListe } from '~components/person/uhaandtereHendelser/utils'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/behandling'

export const OpprettNyBehandling = ({ sakId }: { sakId: number }) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [valgtRevurdering, setValgtRevurdering] = useState<string | undefined>()
  const stoettedeRevurderinger = stoettedeRevurderingerSomListe()

  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)
  const opprettBehandling = () => {
    if (valgtRevurdering === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      { sakId: sakId, aarsak: valgtRevurdering },
      () => {
        location.reload()
      },
      (err) => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
        console.error(`Feil ved opprettelse av revurdering, status: ${err.status} err: ${err.error}`)
      }
    )
  }

  return (
    <MaxWidth>
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
            onChange={(e) => setValgtRevurdering(e.target.value)}
            error={error}
          >
            <option>Velg type</option>
            {stoettedeRevurderinger.map((revurdering, i) => {
              return (
                <option key={`revurdering${i}`} value={revurdering}>
                  {revurdering}
                </option>
              )
            })}
          </Select>
          <MarginTop15>
            <MarginRight15>
              <Button loading={isPending(opprettRevurderingStatus)} onClick={opprettBehandling}>
                Opprett
              </Button>
            </MarginRight15>
            <Button
              variant="secondary"
              onClick={() => {
                setOpen(false)
                resetApiCall()
              }}
            >
              Avbryt
            </Button>
          </MarginTop15>
        </Modal.Content>
      </Modal>
    </MaxWidth>
  )
}

const MarginTop15 = styled.div`
  margin-top: 15px;
`

const MarginRight15 = styled.span`
  margin-right: 15px;
`

const MaxWidth = styled.p`
  max-width: 500px;
`
