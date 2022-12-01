import { Button, Modal, Tag } from '@navikt/ds-react'
import { useState } from 'react'
import { genererPdf } from '~shared/api/brev'
import styled from 'styled-components'
import { Delete, Findout, Notes, Success } from '@navikt/ds-icons'
import { PdfVisning } from './pdf-visning'
import Spinner from '~shared/Spinner'

const ButtonRow = styled.div`
  background: white;
  //overflow: hidden;
  width: 100%;
  text-align: right;
`

export default function BrevModal({
  brev,
  ferdigstill,
  slett,
}: {
  brev: any
  ferdigstill: (brevId: any) => Promise<void>
  slett: (brevId: any) => Promise<void>
}) {
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [hasLoaded, setHasLoaded] = useState<boolean>(false)

  const isDone = ['FERDIGSTILT', 'JOURNALFOERT', 'DISTRIBUERT'].includes(brev.status)

  const open = () => {
    setIsOpen(true)

    genererPdf(brev.id)
      .then((res) => {
        if (res.status === 'ok') {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.error)
        }
      })
      .then((file) => URL.createObjectURL(file!!))
      .then((url) => setFileURL(url))
      .catch((e) => setError(e.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
        setHasLoaded(true)
      })
  }

  const ferdigstillBrev = () => ferdigstill(brev.id).then(() => setIsOpen(false))

  const slettBrev = () => slett(brev.id).then(() => setIsOpen(false))

  return (
    <>
      <Button variant={isDone || brev.erVedtaksbrev ? 'secondary' : 'primary'} size={'small'} onClick={open}>
        {isDone || brev.erVedtaksbrev ? <Findout /> : <Notes />}
      </Button>
      &nbsp;&nbsp;
      {!isDone && !brev.erVedtaksbrev && (
        <Button variant={'danger'} size={'small'} disabled={isDone} onClick={slettBrev}>
          <Delete />
        </Button>
      )}
      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <h2>{brev.tittel}</h2>
          <h4>
            <Tag variant={'info'} size={'small'}>
              {brev.status}
            </Tag>
          </h4>

          <PdfVisning fileUrl={fileURL} error={error} />
          <Spinner visible={!hasLoaded} label="Laster inn PDF" />

          <ButtonRow>
            <Button variant={'secondary'} onClick={() => setIsOpen(false)}>
              {isDone || brev.erVedtaksbrev ? 'Lukk' : 'Avbryt'}
            </Button>

            {!isDone && !brev.erVedtaksbrev && (
              <>
                &nbsp;&nbsp;
                <Button variant={'primary'} onClick={ferdigstillBrev}>
                  Godkjenn <Success />
                </Button>
              </>
            )}
          </ButtonRow>
        </Modal.Content>
      </Modal>
    </>
  )
}
