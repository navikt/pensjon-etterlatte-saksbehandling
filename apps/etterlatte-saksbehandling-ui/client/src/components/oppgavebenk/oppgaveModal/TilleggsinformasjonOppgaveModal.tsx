import { Alert, BodyShort, Button, HGrid, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { DokumentVisningModal, PdfVisning } from '~shared/brev/PdfVisning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumentPDF, hentJournalpost } from '~shared/api/dokument'
import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { isPending } from '~shared/api/apiUtils'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { useForm } from 'react-hook-form'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import Spinner from '~shared/Spinner'
import { EyeIcon } from '@navikt/aksel-icons'

export const TilleggsinformasjonOppgaveModal = ({
  oppgave,
  oppdaterStatus,
  oppdaterMerknad,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [isOpen, setIsOpen] = useState(false)
  const [fileURL, setFileURL] = useState<string>()

  const [journalpostResult, apiHentJournalpost] = useApiCall(hentJournalpost)
  const [pdfResult, apiHentDokumentPDF] = useApiCall(hentDokumentPDF)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  useEffect(() => {
    if (isOpen) {
      apiHentJournalpost(oppgave.referanse!, (journalpost) => {
        apiHentDokumentPDF(
          {
            journalpostId: journalpost.journalpostId,
            dokumentInfoId: journalpost.dokumenter[0].dokumentInfoId,
          },
          (bytes) => {
            const blob = new Blob([bytes], { type: 'application/pdf' })

            setFileURL(URL.createObjectURL(blob))
          }
        )
      })
    }
  }, [isOpen])

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    const merknad = `${oppgave.merknad}. Kommentar: ${kommentar}`

    avsluttOppgave({ id: oppgave.id, merknad }, (oppgave) => {
      oppdaterStatus(oppgave.id, oppgave.status)
      oppdaterMerknad(oppgave.id, merknad)
      setIsOpen(false)
    })
  }

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  return (
    <>
      <Button onClick={() => setIsOpen(true)} size="small" icon={<EyeIcon aria-hidden />}>
        Se oppgave
      </Button>
      <DokumentVisningModal
        open={isOpen}
        onClose={() => setIsOpen(false)}
        aria-label="Tilleggsinformasjon"
        header={{ heading: 'Vurder tilleggsinformasjon' }}
      >
        <Modal.Body>
          <HGrid gap="space-6" columns={{ md: 'auto 400px' }}>
            {isPending(journalpostResult) || isPending(pdfResult) ? (
              <Spinner label="Henter journalført søknad..." />
            ) : (
              <PdfVisning fileUrl={fileURL} />
            )}

            <VStack gap="space-4" justify="end">
              <Alert variant="warning">{oppgave.merknad}</Alert>

              {kanRedigeres &&
                (erTildeltSaksbehandler ? (
                  <Textarea
                    {...register('kommentar', {
                      required: {
                        value: true,
                        message: 'Du må legge til en kommentar',
                      },
                      minLength: {
                        value: 10,
                        message: 'Kommentaren må bestå av minst 10 tegn',
                      },
                    })}
                    label="Kommentar"
                    error={errors.kommentar?.message}
                  />
                ) : (
                  <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
                ))}
            </VStack>
          </HGrid>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4" justify="end">
            <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(ferdigstillOppgaveStatus)}>
              Lukk
            </Button>

            {kanRedigeres && erTildeltSaksbehandler && (
              <Button
                data-color="danger"
                variant="primary"
                onClick={handleSubmit(avslutt)}
                loading={isPending(ferdigstillOppgaveStatus)}
                disabled={!fileURL}
              >
                Avslutt oppgave
              </Button>
            )}
          </HStack>
        </Modal.Footer>
      </DokumentVisningModal>
    </>
  )
}
