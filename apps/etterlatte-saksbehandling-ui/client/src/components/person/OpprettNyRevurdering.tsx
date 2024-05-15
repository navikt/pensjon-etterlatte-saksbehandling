import { Alert, Button, Heading, Loader, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { useNavigate } from 'react-router-dom'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { ButtonGroup } from '~shared/styled'
import { ISak } from '~shared/types/sak'

export const OpprettNyRevurdering = ({
  sak,
  oppgaveId,
  begrunnelse,
}: {
  sak: ISak
  oppgaveId?: string
  begrunnelse?: string
}) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [valgtRevurdering, setValgtRevurdering] = useState<Revurderingaarsak | undefined>()
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const [muligeRevurderingAarsakerResult, muligeRevurderingeraarsakerFetch] = useApiCall(hentStoettedeRevurderinger)
  const [opprettRevurderingStatus, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)

  const navigate = useNavigate()
  const opprettBehandling = () => {
    if (valgtRevurdering === undefined) {
      return setError('Du må velge en revurdering')
    }
    opprettRevurdering(
      {
        sakId: sak.id,
        aarsak: valgtRevurdering,
        fritekstAarsak: fritekstgrunn || null,
        begrunnelse: begrunnelse,
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
    resetApiCall()
  }

  useEffect(() => {
    muligeRevurderingeraarsakerFetch({ sakType: sak.sakType })
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
                value={valgtRevurdering}
                onChange={(e) => setValgtRevurdering(e.target.value as Revurderingaarsak)}
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
              {valgtRevurdering &&
                [Revurderingaarsak.ANNEN, Revurderingaarsak.ANNEN_UTEN_BREV].includes(valgtRevurdering) && (
                  <VStack gap="10" style={{ marginTop: '2rem' }}>
                    <TextField
                      label="Beskriv årsak"
                      size="medium"
                      type="text"
                      value={fritekstgrunn}
                      onChange={(e) => setFritekstgrunn(e.target.value)}
                    />
                    <Alert variant="warning" style={{ maxWidth: '20em' }}>
                      Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen.
                    </Alert>
                  </VStack>
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
