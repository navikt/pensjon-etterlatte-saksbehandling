import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentJournalpost } from '~shared/api/dokument'
import { Alert, BodyShort, Button, HStack, Modal, Select, Textarea, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { Link } from 'react-router-dom'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { SakType } from '~shared/types/sak'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { useForm } from 'react-hook-form'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

enum HandlingValgt {
  AVSLUTT_OPPGAVE,
  OPPRETT_REVURDERING,
  INGEN,
}

interface Skjema {
  revurderingsaarsak: Revurderingaarsak
  begrunnelse: string
}

interface Props {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const MeldtInnEndringOppgaveModal = ({ oppgave, oppdaterStatus }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [open, setOpen] = useState<boolean>(false)
  const [handlingValgt, setHandlingValgt] = useState<HandlingValgt>(HandlingValgt.INGEN)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Skjema>()

  const [hentJournalpostResult, hentJournalpostFetch] = useApiCall(hentJournalpost)
  const [hentStoettedeRevurderingsAarsakerResult, hentStoettedeRevurderingsAarsakerFetch] =
    useApiCall(hentStoettedeRevurderinger)

  const [ferdigstillOppgaveResult, ferdigstillOppgavePost] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [opprettRevurderingResult, opprettRevurderingPost] = useApiCall(opprettRevurderingApi)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const sorterRevurderingsaarsakerKronologisk = (revurderingsaarsaker: Revurderingaarsak[]): Revurderingaarsak[] => {
    return revurderingsaarsaker.toSorted((first, last) => {
      if (tekstRevurderingsaarsak[first].trim().toLowerCase() > tekstRevurderingsaarsak[last].trim().toLowerCase()) {
        return 1
      }
      return -1
    })
  }

  const lukkModal = () => {
    setHandlingValgt(HandlingValgt.INGEN)
    setOpen(false)
  }

  const avsluttOppgaveEllerOpprettRevurdering = (data: Skjema) => {
    if (handlingValgt === HandlingValgt.AVSLUTT_OPPGAVE) {
      ferdigstillOppgavePost({ id: oppgave.id, merknad: data.begrunnelse }, () => {
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
        lukkModal()
      })
    } else if (handlingValgt === HandlingValgt.OPPRETT_REVURDERING) {
      opprettRevurderingPost({ sakId: oppgave.sakId, aarsak: data.revurderingsaarsak }, () => {
        ferdigstillOppgavePost({ id: oppgave.id, merknad: data.begrunnelse }, () => {
          oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
          lukkModal()
        })
      })
    }
  }

  useEffect(() => {
    if (open) hentJournalpostFetch(oppgave.referanse!)
  }, [open])

  useEffect(() => {
    if (handlingValgt) hentStoettedeRevurderingsAarsakerFetch({ sakType: SakType.OMSTILLINGSSTOENAD })
  }, [handlingValgt])

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal
        open={open}
        aria-labelledby="Meldt inn endring oppgave modal"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Melding om endring' }}
      >
        <Modal.Body>
          <form onSubmit={handleSubmit(avsluttOppgaveEllerOpprettRevurdering)}>
            <VStack gap="4">
              {kanRedigeres &&
                (erTildeltSaksbehandler ? (
                  <>
                    {mapResult(hentJournalpostResult, {
                      pending: <Spinner label="Henter journalpost..." />,
                      success: (journalpost) => (
                        <Alert variant="info">
                          <VStack gap="2">
                            <BodyShort>
                              Bruker har meldt inn en endring. Velg mellom å opprette en revurdering eller å avslutte
                              oppgaven dersom den ikke skal behandles.
                            </BodyShort>
                            <div>
                              <Button
                                as={Link}
                                icon={<ExternalLinkIcon aria-hidden />}
                                size="small"
                                to={`/api/dokumenter/${journalpost.journalpostId}/${journalpost.dokumenter[0].dokumentInfoId}`}
                                target="_blank"
                              >
                                Åpne dokument (åpnes i ny fane)
                              </Button>
                            </div>
                          </VStack>
                        </Alert>
                      ),
                    })}

                    {handlingValgt === HandlingValgt.OPPRETT_REVURDERING && (
                      <>
                        {mapResult(hentStoettedeRevurderingsAarsakerResult, {
                          pending: <Spinner label="Henter revurderingsårsaker..." />,
                          success: (revurderingsaarsaker) => (
                            <>
                              <Select
                                {...register('revurderingsaarsak', {
                                  required: {
                                    value: true,
                                    message: 'Du må velge en årsak for revurdering',
                                  },
                                })}
                                label="Årsak til revurdering"
                                error={errors.revurderingsaarsak?.message}
                              >
                                <option value="">Velg årsak</option>
                                {sorterRevurderingsaarsakerKronologisk(revurderingsaarsaker).map((aarsak, index) => (
                                  <option key={index} value={aarsak}>
                                    {tekstRevurderingsaarsak[aarsak]}
                                  </option>
                                ))}
                              </Select>
                            </>
                          ),
                        })}
                      </>
                    )}

                    {handlingValgt !== HandlingValgt.INGEN && (
                      <Textarea
                        {...register('begrunnelse', {
                          required: {
                            value: true,
                            message: 'Du må gi en begrunnelse',
                          },
                        })}
                        label="Begrunnelse"
                        error={errors.begrunnelse?.message}
                      />
                    )}
                  </>
                ) : (
                  <Alert variant="warning">Du må tildele deg oppgaven for å endre den</Alert>
                ))}

              {isFailureHandler({
                apiResult: ferdigstillOppgaveResult,
                errorMessage: 'Feil under ferdigstilling av oppgave',
              })}
              {isFailureHandler({
                apiResult: opprettRevurderingResult,
                errorMessage: 'Feil under opprettelse av revurdering',
              })}

              <HStack justify="space-between">
                <Button variant="tertiary" onClick={lukkModal}>
                  Avbryt
                </Button>
                {kanRedigeres && erTildeltSaksbehandler && (
                  <>
                    {handlingValgt === HandlingValgt.INGEN && (
                      <HStack gap="4">
                        <Button
                          variant="secondary"
                          type="button"
                          onClick={() => setHandlingValgt(HandlingValgt.AVSLUTT_OPPGAVE)}
                        >
                          Avslutt oppgave
                        </Button>
                        <Button type="button" onClick={() => setHandlingValgt(HandlingValgt.OPPRETT_REVURDERING)}>
                          Opprett revurdering
                        </Button>
                      </HStack>
                    )}
                    {handlingValgt === HandlingValgt.AVSLUTT_OPPGAVE && (
                      <HStack gap="4">
                        <Button variant="secondary" type="button" onClick={() => setHandlingValgt(HandlingValgt.INGEN)}>
                          Tilbake
                        </Button>
                        <Button loading={isPending(ferdigstillOppgaveResult)}>Avslutt</Button>
                      </HStack>
                    )}
                    {handlingValgt === HandlingValgt.OPPRETT_REVURDERING && (
                      <HStack gap="4">
                        <Button variant="secondary" type="button" onClick={() => setHandlingValgt(HandlingValgt.INGEN)}>
                          Tilbake
                        </Button>
                        <Button loading={isPending(opprettRevurderingResult || ferdigstillOppgaveResult)}>
                          Opprett
                        </Button>
                      </HStack>
                    )}
                  </>
                )}
              </HStack>
            </VStack>
          </form>
        </Modal.Body>
      </Modal>
    </>
  )
}
