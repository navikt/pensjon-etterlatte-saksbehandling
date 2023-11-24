import { Button, Modal } from '@navikt/ds-react'
import { FilePdfIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'

export default function BrevModal({ brev }: { brev: IBrev }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button variant="secondary" title="Vis PDF" icon={<FilePdfIcon />} onClick={() => setOpen(true)} />

      <Modal open={open} onClose={() => setOpen(false)} style={{ maxWidth: '100%' }}>
        <Modal.Body style={{ minWidth: '60rem', paddingTop: '3rem' }}>
          {open && <ForhaandsvisningBrev brev={brev} />}
        </Modal.Body>
      </Modal>
    </>
  )
}
