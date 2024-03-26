import { useState } from 'react'
import { Modal } from '@navikt/ds-react'
import { Brevutfall } from '~components/behandling/brevutfall/Brevutfall'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

export default function BrevutfallModal(props: {
  behandling: IBehandlingReducer
  onLagre: () => void
  setVis: (b: boolean) => void
}) {
  const [isOpen, setIsOpen] = useState(true)

  return (
    <Modal
      open={isOpen}
      onClose={() => {
        setIsOpen(false)
        props.onLagre()
        props.setVis(false)
      }}
      aria-labelledby="modal-heading"
    >
      <Modal.Header>Registrer eller rediger brevutfall</Modal.Header>
      <Modal.Body>
        <Brevutfall behandling={props.behandling} resetBrevutfallvalidering={() => {}} />
      </Modal.Body>
    </Modal>
  )
}
