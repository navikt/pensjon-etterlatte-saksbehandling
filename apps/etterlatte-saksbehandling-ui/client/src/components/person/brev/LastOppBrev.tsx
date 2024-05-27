import { Alert, Button, Heading, HStack, Link, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { DownloadIcon, UploadIcon } from '@navikt/aksel-icons'
import { opprettBrevFraPDF } from '~shared/api/brev'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { DokumentVisningModal, PdfVisning } from '~shared/brev/pdf-visning'
import { isPending } from '~shared/api/apiUtils'
import { ISak } from '~shared/types/sak'

export const LastOppBrev = ({ sak }: { sak: ISak }) => {
  const navigate = useNavigate()

  const [isOpen, setIsOpen] = useState(false)
  const [filURL, setFilURL] = useState<string | undefined>()
  const [valgtFil, setValgtFil] = useState<File | undefined>()

  const [lastOppStatus, lastOppBrev] = useApiCall(opprettBrevFraPDF)

  const onFileChange = (event: any) => {
    const fil = event.target.files[0]
    setValgtFil(fil)

    const url = URL.createObjectURL(fil)
    setFilURL(url)
  }

  const lagre = () => {
    const formData = new FormData()
    formData.append('fil', valgtFil as Blob, valgtFil!!.name)
    formData.append(
      'data',
      JSON.stringify({
        sak,
        innhold: {
          tittel: valgtFil?.name.replace('.pdf', ''),
          spraak: 'nb',
        },
      })
    )

    lastOppBrev({ sakId: sak.id, formData }, (brev) => {
      navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
    })
  }

  const avbryt = () => {
    setIsOpen(false)
    setFilURL(undefined)
    setValgtFil(undefined)
  }

  return (
    <>
      <Button variant="secondary" icon={<UploadIcon />} iconPosition="right" onClick={() => setIsOpen(true)}>
        Last opp fil
      </Button>

      <DokumentVisningModal open={isOpen} onClose={avbryt} aria-label="Last opp fil">
        <Modal.Header>
          <Heading size="medium" spacing>
            Last opp fil
          </Heading>
        </Modal.Header>
        <Modal.Body>
          {!valgtFil && (
            <>
              <Alert variant="warning" inline>
                Denne funksjonen skal kun brukes i de tilfeller det ikke finnes en brevmal i Gjenny. Vær obs på at
                PDF-en du laster opp må inneholde NAV-logo, søker sitt navn, fødselsnummer, samt saksnummer og dato for
                utsending.
                <br />
                {/* */}
                <Link href="/tom-brevmal.docx" target="_blank">
                  Last ned mal <DownloadIcon />
                </Link>
              </Alert>
              <br />
              <br />
            </>
          )}

          <input type="file" name="file" id="file" onChange={onFileChange} accept="application/pdf" />

          <br />
          <br />

          <PdfVisning fileUrl={filURL} />

          <HStack gap="4" justify="end">
            <Button variant="secondary" onClick={avbryt} disabled={isPending(lastOppStatus)}>
              Avbryt
            </Button>

            <Button onClick={lagre} disabled={!valgtFil} loading={isPending(lastOppStatus)}>
              Lagre
            </Button>
          </HStack>
        </Modal.Body>
      </DokumentVisningModal>
    </>
  )
}
