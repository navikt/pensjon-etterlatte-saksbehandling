import { BodyLong, Button, Modal, Tag } from "@navikt/ds-react";
import { useState } from "react";
import { genererPdf } from "../../../shared/api/brev";
import styled from "styled-components";
import { Delete, Findout, Notes, Success } from "@navikt/ds-icons";

const PdfViewer = styled.iframe`
  //margin-top: 60px;
  margin-bottom: 20px;
  width: 800px;
  height: 1080px;
`

const ButtonRow = styled.div`
  background: white;
  //overflow: hidden;
  width: 100%;
  text-align: right;
`

export default function BrevModal({ brev, ferdigstill, slett }: {
  brev: any
  ferdigstill: (brevId: any) => Promise<void>
  slett: (brevId: any) => Promise<void>
}) {
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [isOpen, setIsOpen] = useState<boolean>(false)

  const isDone = ['FERDIGSTILT', 'SENDT'].includes(brev.status)

  const open = () => {
    setIsOpen(true)

    genererPdf(brev.id)
        .then(file => URL.createObjectURL(file))
        .then(url => setFileURL(url))
        .catch(e => setError(e.message))
        .finally(() => {
          if (fileURL) URL.revokeObjectURL(fileURL)
        })
  }

  const ferdigstillBrev = () => ferdigstill(brev.id).then(() => setIsOpen(false))

  const slettBrev = () => slett(brev.id).then(() => setIsOpen(false))

  return (
      <>
        <Button variant={isDone ? 'secondary' : 'primary'} size={'small'} onClick={open}>
          {isDone ? <Findout /> : <Notes />}
        </Button>
        &nbsp;&nbsp;
        <Button variant={'danger'} size={'small'} disabled={isDone} onClick={slettBrev}>
          <Delete />
        </Button>

        <Modal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
            <h2>{brev.tittel}</h2>
            <h4>
              <Tag variant={'info'} size={'small'}>
                {brev.status}
              </Tag>
            </h4>

            {error && (
                <BodyLong>
                  En feil har oppstått ved henting av PDF:
                  <br/>
                  <code>{error}</code>
                </BodyLong>
            )}

            <div>
              {fileURL && <PdfViewer src={fileURL}/>}
            </div>

            <ButtonRow>
              <Button variant={'secondary'} onClick={() => setIsOpen(false)}>
                {isDone ? 'Lukk' : 'Avbryt'}
              </Button>

              {!isDone && (
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
