import { Button, Modal } from '@navikt/ds-react'
import { FilePdfIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'

export default function BrevModal({ brev }: { brev: IBrev }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button variant={'secondary'} title={'Vis PDF'} icon={<FilePdfIcon />} onClick={() => setOpen(true)} />

      <Modal open={open} onClose={() => setOpen(false)}>
        <Modal.Content style={{ minWidth: '60rem', paddingTop: '3rem' }}>
          <ForhaandsvisningBrev brev={brev} />
        </Modal.Content>
      </Modal>
    </>
  )
}
