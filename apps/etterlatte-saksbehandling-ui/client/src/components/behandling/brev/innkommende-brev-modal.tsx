import { Button, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { hentInnkommendeBrevInnhold } from '../../../shared/api/brev'
import styled from 'styled-components'
import { Findout } from '@navikt/ds-icons'
import {PdfVisning} from "./pdf-visning";

const ButtonRow = styled.div`
  background: white;
  //overflow: hidden;
  width: 100%;
  text-align: right;
`

export default function InnkommendeBrevModal({
    tittel,
    journalpostId,
    dokumentInfoId,
}: {
    tittel: string
    journalpostId: string
    dokumentInfoId: string
}) {
    const [error, setError] = useState<string>()
    const [fileURL, setFileURL] = useState<string>('')
    const [isOpen, setIsOpen] = useState<boolean>(false)

    const open = (journalpostId: string, dokumentInfoId: string) => {
        setIsOpen(true)

        hentInnkommendeBrevInnhold(journalpostId, dokumentInfoId)
            .then((file) => URL.createObjectURL(file))
            .then((url) => setFileURL(url))
            .catch((e) => setError(e.message))
            .finally(() => {
                if (error) URL.revokeObjectURL(fileURL)
            })
    }

    return (
        <>
            <Button variant={ 'secondary'} size={'small'} onClick={() => open(journalpostId, dokumentInfoId)}>
                 <Findout/>
            </Button>

            <Modal open={isOpen} onClose={() => setIsOpen(false)}>
                <Modal.Content>
                    <h2>{tittel}</h2>

                    <PdfVisning fileUrl={fileURL} error={error} />

                    <ButtonRow>
                        <Button variant={'secondary'} onClick={() => setIsOpen(false)}>
                            Lukk
                        </Button>
                    </ButtonRow>
                </Modal.Content>
            </Modal>
        </>
    )
}
