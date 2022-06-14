import { BodyLong, Button, Modal } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { genererPdf, opprettEllerOppdaterBrevForVedtak } from "../../../shared/api/brev";
import styled from "styled-components";
import { FileIcon } from "../../../shared/icons/fileIcon";
import { useParams } from "react-router-dom";

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

const VedtaksbrevWrapper = styled.div`
  display: inline-flex;
  margin-top: 8em;
  cursor: pointer;

  .text {
    line-height: 1.5em;
    margin-left: 0.3em;
    color: #0056b4;
    font-size: 18px;
    font-weight: 600;
    text-decoration-line: underline;
  }
`

export default function BrevModal() {
  const { behandlingId } = useParams()

  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [brevId, setBrevId] = useState<string>()

  const preview = () => {
    setIsOpen(true)

    genererPdf(brevId!!)
        .then(file => URL.createObjectURL(file))
        .then(url => setFileURL(url))
        .catch(e => setError(e.message))
        .finally(() => {
          if (fileURL) URL.revokeObjectURL(fileURL)
        })
  }

  useEffect(() => {
    opprettEllerOppdaterBrevForVedtak(behandlingId!!)
        .then((id) => setBrevId(id))
  }, [])

  return (
      <>
        <VedtaksbrevWrapper onClick={preview}>
          <FileIcon/>
          <span className="text">Vis vedtaksbrev</span>
        </VedtaksbrevWrapper>

        <Modal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
            {/*<h2>{brev.tittel}</h2>
            <h4>
              <Tag variant={'info'} size={'small'}>
                {brev.status}
              </Tag>
            </h4>*/}

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
                Lukk
              </Button>
            </ButtonRow>
          </Modal.Content>
        </Modal>
      </>
  )
};
