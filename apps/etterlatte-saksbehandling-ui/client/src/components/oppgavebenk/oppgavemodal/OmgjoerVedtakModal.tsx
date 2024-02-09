import { OppgaveDTO } from '~shared/api/oppgaver'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isInitial, isPending, isSuccess } from '~shared/api/apiUtils'

function opprettRevurderingForOmgjoering(args: {
  sakId: number
  oppgaveId: string
}): Promise<ApiResponse<IDetaljertBehandling>> {
  return apiClient.post(`/sak/${args.sakId}/omgjoering/${args.oppgaveId}`, {})
}

export function OmgjoerVedtakModal(props: { oppgave: OppgaveDTO }) {
  const { oppgave } = props
  const [open, setOpen] = useState(false)
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettRevurderingForOmgjoering)

  function opprett() {
    if (isInitial(opprettRevurderingStatus)) {
      opprettRevurdering({
        oppgaveId: oppgave.id,
        sakId: oppgave.sakId,
      })
    }
  }

  return (
    <>
      <Button variant="primary" size="small" onClick={() => setOpen(true)} style={{ textAlign: 'left' }}>
        Omgjør vedtak
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Omgjør vedtak
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <BodyLong>{oppgave.type}</BodyLong>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="primary"
            onClick={opprett}
            loading={isPending(opprettRevurderingStatus)}
            disabled={isSuccess(opprettRevurderingStatus)}
          >
            Opprett revurdering
          </Button>
          <Button variant="tertiary" onClick={() => setOpen(false)} disabled={isPending(opprettRevurderingStatus)}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
