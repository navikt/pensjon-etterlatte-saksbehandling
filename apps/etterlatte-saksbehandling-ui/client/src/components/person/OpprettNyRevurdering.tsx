import { Alert, Button, Heading, Loader, Modal, Select, TextField } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { useNavigate } from 'react-router-dom'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ButtonGroup, SpaceChildren } from '~shared/styled'
import { SakType } from '~shared/types/sak'
import styled from 'styled-components'

export const OpprettNyRevurdering = ({
  sakId,
  sakType,
  oppgaveId,
  begrunnelse,
}: {
  sakId: number
  sakType: SakType
  oppgaveId?: string
  begrunnelse?: string
}) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [revurderingaarsak, setRevurderingaarsak] = useState<Revurderingaarsak | undefined>()
  const [fritekstAarsak, setFritekstAarsak] = useState<string>('')

  const [muligeRevurderingAarsakerResult, muligeRevurderingeraarsakerFetch] = useApiCall(hentStoettedeRevurderinger)
  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)

  const navigate = useNavigate()
  const opprettBehandling = () => {
    if (revurderingaarsak === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      {
        sakId: sakId,
        aarsak: revurderingaarsak,
        fritekstAarsak,
        begrunnelse,
        paaGrunnAvOppgaveId: oppgaveId,
      },
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
    setError('')
    resetApiCall()
  }

  useEffect(() => {
    muligeRevurderingeraarsakerFetch({ sakType })
  }, [])

  return mapResult(muligeRevurderingAarsakerResult, {
    pending: <Loader />,
    success: (muligeRevurderingAarsaker) =>
      !!muligeRevurderingAarsaker?.length ? (
        <>
          <Button
            variant={oppgaveId ? 'primary' : 'secondary'}
            size={oppgaveId ? 'small' : 'medium'}
            onClick={() => setOpen(true)}
          >
            Opprett ny revurdering
          </Button>
          <Modal open={open} onClose={closeAndReset} aria-labelledby="modal-heading">
            <Modal.Header closeButton={false}>
              <Heading level="2" size="medium" id="modal-heading">
                Opprett ny revurdering
              </Heading>
            </Modal.Header>
            <Modal.Body>
              <Select
                label="Årsak til revurdering"
                value={revurderingaarsak}
                onChange={(e) => setRevurderingaarsak(e.target.value as Revurderingaarsak)}
                error={error}
              >
                <option>Velg type</option>
                {muligeRevurderingAarsaker.map((revurdering, i) => {
                  return (
                    <option key={`revurdering${i}`} value={revurdering}>
                      {tekstRevurderingsaarsak[revurdering]}
                    </option>
                  )
                })}
              </Select>
              {revurderingaarsak === Revurderingaarsak.ANNEN && (
                <AnnenRevurderingWrapper>
                  <SpaceChildren direction="column">
                    <TextField
                      label="Beskriv årsak"
                      size="medium"
                      type="text"
                      value={fritekstAarsak}
                      onChange={(e) => setFritekstAarsak(e.target.value)}
                    />
                    <AnnetRevurderingAlert variant="warning" size="small" inline>
                      Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen
                    </AnnetRevurderingAlert>
                  </SpaceChildren>
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
      ) : (
        <Alert variant="info" inline>
          Ingen mulige revurderinger for denne saken
        </Alert>
      ),
  })
}

const AnnenRevurderingWrapper = styled.div`
  margin-top: 1rem;
`

const AnnetRevurderingAlert = styled(Alert)`
  max-width: 20rem;
`
