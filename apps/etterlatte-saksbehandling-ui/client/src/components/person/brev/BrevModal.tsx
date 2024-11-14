import { Button, Heading, Modal } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'

export default function BrevModal({ brev }: { brev: IBrev }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button variant="primary" icon={<EyeIcon />} onClick={() => setOpen(true)} size="small">
        Vis
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        style={{ maxWidth: '100%' }}
        closeOnBackdropClick={true}
        aria-label={brev.tittel}
      >
        <Modal.Header>
          <Heading size="large">{brev.tittel}</Heading>
        </Modal.Header>
        <Modal.Body style={{ minWidth: '60rem', paddingTop: '3rem' }}>
          {open && <ForhaandsvisningBrev brev={brev} />}
        </Modal.Body>
      </Modal>
    </>
  )
}
