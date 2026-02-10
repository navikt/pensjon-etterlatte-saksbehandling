import { Alert, Button, Heading, HStack, Link, Modal, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { DownloadIcon, FileResetIcon, UploadIcon } from '@navikt/aksel-icons'
import { opprettBrevFraPDF } from '~shared/api/brev'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { DokumentVisningModal, PdfVisning } from '~shared/brev/PdfVisning'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ISak } from '~shared/types/sak'
import { round } from 'lodash'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ClickEvent, trackClick } from '~utils/analytics'

/**
 * Husk å endre [proxy_body_size] i nais-filene hvis du skal øke maks filstørrelse.
 * Mer info: https://docs.nais.io
 * https://docs.nais.io/workloads/application/reference/ingress/?h=body+size#custom-max-body-size
 **/
const MAKS_FILSTOERRELSE_MB = 2

export const LastOppBrev = ({ sak }: { sak: ISak }) => {
  const navigate = useNavigate()

  const [isOpen, setIsOpen] = useState(false)
  const [filURL, setFilURL] = useState<string | undefined>()
  const [valgtFil, setValgtFil] = useState<File | undefined>()
  const [error, setError] = useState<string>()

  const [lastOppStatus, lastOppBrev] = useApiCall(opprettBrevFraPDF)

  const tilbakestill = () => {
    setError(undefined)
    setValgtFil(undefined)
    setFilURL(undefined)
  }

  const onFileChange = (event: any) => {
    const fil = event.target.files[0]
    if (!fil) {
      return
    }

    const filstoerrelseMegabytes = fil.size / (1024 * 1024)

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

    trackClick(ClickEvent.LAST_OPP_BREV)

    lastOppBrev({ sakId: sak.id, formData }, (brev) => {
      navigate(`/person/sak/${brev.sakId}/brev/${brev.id}`)
    })
  }

  const avbryt = () => {
    setIsOpen(false)
    setFilURL(undefined)
    setValgtFil(undefined)
  }

  return (
    <>
      <Button
        variant="secondary"
        icon={<UploadIcon aria-hidden />}
        iconPosition="right"
        onClick={() => setIsOpen(true)}
      >
        Last opp fil
      </Button>

      <DokumentVisningModal open={isOpen} onClose={avbryt} aria-label="Last opp fil">
        <Modal.Header>
          <Heading size="medium" spacing>
            Last opp fil
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <VStack gap="space-4">
            {!valgtFil && (
              <>
                <Alert variant="warning" inline>
                  Denne funksjonen skal kun brukes i de tilfeller det ikke finnes en brevmal i Gjenny. Vær obs på at
                  PDF-en du laster opp må inneholde NAV-logo, søker sitt navn, fødselsnummer, samt saksnummer og dato
                  for utsending. Filen du laster opp kan heller ikke være større enn {MAKS_FILSTOERRELSE_MB}MB
                  <br />
                  {/* */}
                  <Link href="/tom-brevmal.docx" target="_blank">
                    Last ned mal <DownloadIcon aria-hidden />
                  </Link>
                </Alert>
                <br />
                <br />
              </>
            )}

            <input type="file" name="file" id="file" onChange={onFileChange} accept="application/pdf" />

            {error && <Alert variant="error">{error}</Alert>}

            <PdfVisning fileUrl={filURL} />

            <HStack justify="space-between">
              <div>
                {!!filURL && (
                  <Button
                    variant="secondary"
                    onClick={tilbakestill}
                    disabled={isPending(lastOppStatus)}
                    icon={<FileResetIcon aria-hidden />}
                  >
                    Tilbakestill
                  </Button>
                )}
              </div>
              {isFailure(lastOppStatus) && (
                <ApiErrorAlert>
                  {lastOppStatus.error.detail
                    ? lastOppStatus.error.detail
                    : `Kunne ikke laste oppe brev, status ${lastOppStatus.status}`}
                </ApiErrorAlert>
              )}

              <HStack gap="space-4" justify="end">
                <Button variant="secondary" onClick={avbryt} disabled={isPending(lastOppStatus)}>
                  Avbryt
                </Button>

                <Button onClick={lagre} disabled={!valgtFil} loading={isPending(lastOppStatus)}>
                  Lagre
                </Button>
              </HStack>
            </HStack>
          </VStack>
        </Modal.Body>
      </DokumentVisningModal>
    </>
  )
}
