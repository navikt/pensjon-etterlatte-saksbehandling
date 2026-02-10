import React, { Dispatch, SetStateAction, useState } from 'react'
import { OverstyrBeregning } from '~shared/types/Beregning'
import { BodyShort, Button, Modal, VStack } from '@navikt/ds-react'
import { ArrowUndoIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { deaktiverOverstyrtBeregning } from '~shared/api/beregning'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

interface Props {
  behandlingId: string
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}

export const SkruAvOverstyrtBeregningModal = ({ behandlingId, setOverstyrt }: Props) => {
  const [open, setOpen] = useState<boolean>(false)
  const dispatch = useAppDispatch()

  const [deaktiverOverstyrtBeregningResult, deaktiverOverstyrtBeregningRequest] =
    useApiCall(deaktiverOverstyrtBeregning)

  const skruAvOverstyrtBeregning = () => {
    deaktiverOverstyrtBeregningRequest(behandlingId, () => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
      setOpen(false)
      setOverstyrt(undefined)
    })
  }

  return (
    <>
      <Button size="small" variant="secondary" icon={<ArrowUndoIcon aria-hidden />} onClick={() => setOpen(true)}>
        Skru av overstyrt beregning
      </Button>
      <Modal
        header={{ heading: 'Er du sikker på at du vil skru av overstyrt beregning?' }}
        open={open}
        onClose={() => setOpen(false)}
      >
        <Modal.Body>
          <VStack gap="space-4">
            <BodyShort>
              Beregningsperioder vil bli permanent slettet. Virkningstidspunkt for revurdering MÅ settes tilbake til
              sakens første virkningstidspunkt.
            </BodyShort>
            {isFailureHandler({
              errorMessage: 'Feil under deaktivering av overstyrt beregning',
              apiResult: deaktiverOverstyrtBeregningResult,
            })}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="danger"
            loading={isPending(deaktiverOverstyrtBeregningResult)}
            onClick={skruAvOverstyrtBeregning}
          >
            Skru av
          </Button>
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
