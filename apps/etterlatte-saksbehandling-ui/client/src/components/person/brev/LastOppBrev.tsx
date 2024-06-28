import { Alert, Button, Heading, HStack, Link, Modal, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { DownloadIcon, UploadIcon } from '@navikt/aksel-icons'
import { opprettBrevFraPDF } from '~shared/api/brev'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { DokumentVisningModal, PdfVisning } from '~shared/brev/pdf-visning'
import { isPending } from '~shared/api/apiUtils'
import { ISak } from '~shared/types/sak'
import { round } from 'lodash'

const MAKS_FILSTOERRELSE_MB = 1

export const LastOppBrev = ({ sak }: { sak: ISak }) => {
  const navigate = useNavigate()

  const [isOpen, setIsOpen] = useState(false)
  const [filURL, setFilURL] = useState<string | undefined>()
  const [valgtFil, setValgtFil] = useState<File | undefined>()
  const [error, setError] = useState<string>()

  const [lastOppStatus, lastOppBrev] = useApiCall(opprettBrevFraPDF)

  const onFileChange = (event: any) => {
    const fil = event.target.files[0]

    const filstoerrelseMegabytes = fil.size / 1000000

    if (filstoerrelseMegabytes > MAKS_FILSTOERRELSE_MB) {
      setError(`
        Filen du har valgt er for stor (${round(filstoerrelseMegabytes, 1)}MB).
        Kan ikke laste opp filer større enn ${MAKS_FILSTOERRELSE_MB}MB.
      `)
    } else {
      setError(undefined)
      setValgtFil(fil)

      const url = URL.createObjectURL(fil)
      setFilURL(url)
    }
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
          <VStack gap="4">
            {!valgtFil && (
              <>
                <Alert variant="warning" inline>
                  Denne funksjonen skal kun brukes i de tilfeller det ikke finnes en brevmal i Gjenny. Vær obs på at
                  PDF-en du laster opp må inneholde NAV-logo, søker sitt navn, fødselsnummer, samt saksnummer og dato
                  for utsending. Filen du laster opp kan heller ikke være større enn {MAKS_FILSTOERRELSE_MB}MB
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

            {error && <Alert variant="error">{error}</Alert>}

            <PdfVisning fileUrl={filURL} />

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={avbryt} disabled={isPending(lastOppStatus)}>
                Avbryt
              </Button>

              <Button onClick={lagre} disabled={!valgtFil} loading={isPending(lastOppStatus)}>
                Lagre
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </DokumentVisningModal>
    </>
  )
}
