import { BodyLong, Button, Loader, Modal } from "@navikt/ds-react";
import { useState } from "react";
import { genererPdf } from "../../../shared/api/brev";
import { useParams } from "react-router-dom";
import styled from "styled-components";

const PdfViewer = styled.iframe`
  margin: 80px 30px;
  width: 800px;
  height: 1080px;
`

const ButtonRow = styled.div`
  background: white;
  position: absolute;
  bottom: 0;
  width: 100%;
`

export default function BrevModal({ brevId, ferdigstill }: {
  brevId: string
  ferdigstill: (id: any) => Promise<void>
}) {
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [isOpen, setIsOpen] = useState<boolean>(false)

  const open = () => {
    setIsOpen(true)

    genererPdf(brevId)
        .then(file => URL.createObjectURL(file))
        .then(url => setFileURL(url))
        .catch(e => setError(e.message))
        .finally(() => {
          if (fileURL) URL.revokeObjectURL(fileURL)
        })
  }

  const send = () => {
    ferdigstill(brevId)
        .then(() => setIsOpen(false))
  }

  return (
      <>
        <Button
            variant={'primary'}
            size={'small'}
            onClick={open}
        >
          Ferdigstill
        </Button>

        <Modal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
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
                Avbryt
              </Button>

              <Button variant={'primary'} onClick={send}>
                Send
              </Button>
            </ButtonRow>
          </Modal.Content>
        </Modal>
      </>
  )
}
