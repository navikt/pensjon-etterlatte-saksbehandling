import { BodyLong, Button, Modal } from "@navikt/ds-react";
import { useState } from "react";
import { ferdigstillBrev, genererPdf, slettBrev } from "../../../shared/api/brev";
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

export default function BrevModal({ brevId, status }: {
  brevId: string
  status: string
}) {
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [isOpen, setIsOpen] = useState<boolean>(false)

  const isDone = ['FERDIGSTILT', 'SENDT'].includes(status)

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
    ferdigstillBrev(brevId)
        .then(() => setIsOpen(false))
  }

  const slett = () => {
    slettBrev(brevId)
        .then(() => console.log('Slettet ok!'))
  }

  return (
      <>
        <Button variant={isDone ? 'secondary' : 'primary'} size={'small'} onClick={open}>
          {/*{isDone ? "Vis" : "Ferdigstill"}*/}
          {isDone ? <Findout /> : <Notes />}
        </Button>
        &nbsp;&nbsp;
        <Button variant={'danger'} size={'small'} disabled={isDone} onClick={slett}>
          <Delete />
        </Button>

        <Modal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
            <h2>{status}</h2>

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
                    <Button variant={'primary'} onClick={send}>
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
