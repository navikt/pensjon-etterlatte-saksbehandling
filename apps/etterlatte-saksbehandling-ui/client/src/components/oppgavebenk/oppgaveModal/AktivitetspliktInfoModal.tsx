import {
  BodyLong,
  Button,
  Heading,
  HStack,
  Label,
  Modal,
  Radio,
  ReadMore,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { ferdigstillOppgave, hentOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useForm } from 'react-hook-form'
import {
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  AktivitetspliktVurderingValues,
  AktivitetspliktVurderingValuesDefault,
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import {
  hentAktivitspliktVurderingForOppgave,
  opprettAktivitspliktAktivitetsgrad,
  opprettAktivitspliktUnntak,
} from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { Toast } from '~shared/alerts/Toast'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { JaNei } from '~shared/types/ISvar'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { AktivitetspliktVurderingVisning } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurderingVisning'

export const AktivitetspliktInfoModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const [visModal, setVisModal] = useState(false)
  const [erFerdigstilt, setErFerdigstilt] = useState(false)
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurdering>()

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)
  const [opprettetAktivitetsgrad, opprettAktivitetsgrad] = useApiCall(opprettAktivitspliktAktivitetsgrad)
  const [opprettetUnntak, opprettUnntak] = useApiCall(opprettAktivitspliktUnntak)
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForOppgave)
  const [hentOppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
  } = useForm<AktivitetspliktVurderingValues>({
    defaultValues: AktivitetspliktVurderingValuesDefault,
  })

  const ferdigstill = (data: AktivitetspliktVurderingValues) => {
    if (!erFerdigstilt && vurdering) {
      apiFerdigstillOppgave(oppgave.id, () => {
        setVisModal(false)
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
      })
    } else if (data.aktivitetsplikt === JaNei.NEI || data.unntak === JaNei.JA) {
      opprettUnntak(
        {
          sakId: oppgave.sakId,
          oppgaveId: oppgave.id,
          request: {
            id: undefined,
            unntak:
              data.aktivitetsplikt === JaNei.NEI
                ? AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                : (data.midlertidigUnntak as AktivitetspliktUnntakType),
            beskrivelse: data.beskrivelse,
            fom: data.fom ? new Date(data.fom).toISOString() : new Date().toISOString(),
            tom: data.tom && data.aktivitetsplikt === JaNei.JA ? new Date(data.tom).toISOString() : undefined,
          },
        },
        () => {
          apiFerdigstillOppgave(oppgave.id, () => {
            setVisModal(false)
            oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
          })
        }
      )
    } else {
      opprettAktivitetsgrad(
        {
          sakId: oppgave.sakId,
          oppgaveId: oppgave.id,
          request: {
            id: undefined,
            aktivitetsgrad: data.aktivitetsgrad as AktivitetspliktVurderingType,
            beskrivelse: data.beskrivelse,
            fom: data.fom ? new Date(data.fom).toISOString() : new Date().toISOString(),
            tom: data.tom ? new Date(data.tom).toISOString() : undefined,
          },
        },
        () => {
          apiFerdigstillOppgave(oppgave.id, () => {
            setVisModal(false)
            oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
          })
        }
      )
    }
  }

  useEffect(() => {
    if (visModal) {
      hent({ sakId: oppgave.sakId, oppgaveId: oppgave.id }, (result) => {
        setVurdering(result)
        if (result) sjekkOppgaveStatus()
      })
    }
  }, [visModal])

  const sjekkOppgaveStatus = () => {
    apiHentOppgave(oppgave.id, (result) => {
      setErFerdigstilt(result.status === Oppgavestatus.FERDIGSTILT)
    })
  }

  const kanFerdigstilleOppgave = () => {
    if (oppgave.status === Oppgavestatus.UNDER_BEHANDLING && !vurdering) return true
    else if (vurdering && !erFerdigstilt) return true
    return false
  }

  const harAktivitetsplikt = watch('aktivitetsplikt')
  const harUnntak = watch('unntak')

  return (
    <>
      {isSuccess(ferdigstillOppgaveStatus) && (
        <Toast timeout={10000} melding="Oppgave ferdigstilt, har du husket å ferdigstille brev?" />
      )}
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal open={visModal} onClose={() => setVisModal(false)} header={{ heading: 'Vurdering av aktivitetsplikt' }}>
          <Modal.Body>
            <HStack gap="12">
              <Spinner label="Henter vurdering av aktivitetsplikt" visible={isPending(hentet)} />

              {oppgave.status === Oppgavestatus.UNDER_BEHANDLING && !vurdering ? (
                <VStack gap="4">
                  <ControlledRadioGruppe
                    name="aktivitetsplikt"
                    control={control}
                    errorVedTomInput="Du må velge om bruker har aktivitetsplikt"
                    legend="Har bruker aktivitetsplikt?"
                    radios={
                      <>
                        <Radio size="small" value={JaNei.JA}>
                          Ja
                        </Radio>
                        <Radio size="small" value={JaNei.NEI}>
                          {
                            tekstAktivitetspliktUnntakType[
                              AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                            ]
                          }
                        </Radio>
                      </>
                    }
                  />

                  <ReadMore header="Dette mener vi med lav inntekt">
                    Med lav inntekt menes det at den gjenlevende ikke har hatt en gjennomsnittlig årlig arbeidsinntekt
                    som overstiger to ganger grunnbeløpet for hvert av de fem siste årene. I tillegg må den årlige
                    inntekten ikke ha oversteget tre ganger grunnbeløpet hvert av de siste to årene før dødsfallet.
                  </ReadMore>
                  {harAktivitetsplikt === JaNei.JA && (
                    <>
                      <ControlledRadioGruppe
                        name="unntak"
                        control={control}
                        errorVedTomInput="Du må velge om bruker har unntak fra aktivitetsplikt"
                        legend="Er det unntak for bruker?"
                        radios={
                          <>
                            <Radio size="small" value={JaNei.JA}>
                              Ja
                            </Radio>
                            <Radio size="small" value={JaNei.NEI}>
                              Nei
                            </Radio>
                          </>
                        }
                      />
                      {harUnntak === JaNei.JA && (
                        <>
                          <Select
                            label="Hvilket midlertidig unntak er det?"
                            {...register('midlertidigUnntak', {
                              required: { value: true, message: 'Du må velge midlertidig unntak' },
                            })}
                            error={errors.midlertidigUnntak?.message}
                          >
                            <option value="">Velg hvilke unntak</option>
                            {Object.values(AktivitetspliktUnntakType)
                              .filter(
                                (unntak) =>
                                  unntak !== AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                              )
                              .map((type) => (
                                <option key={type} value={type}>
                                  {tekstAktivitetspliktUnntakType[type]}
                                </option>
                              ))}
                          </Select>
                          <div>
                            <Label>Unntaksperiode</Label>
                            <BodyLong>Du trenger ikke legge til en sluttdato hvis den ikke er tilgjengelig</BodyLong>
                          </div>
                          <HStack gap="4">
                            <ControlledDatoVelger name="fom" label="Angi startdato" control={control} />
                            <ControlledDatoVelger
                              name="tom"
                              label="Angi sluttdato"
                              control={control}
                              required={false}
                            />
                          </HStack>
                        </>
                      )}
                      {harUnntak === JaNei.NEI && (
                        <>
                          <Select
                            label="Hva er brukers aktivitetsgrad?"
                            {...register('aktivitetsgrad', {
                              required: { value: true, message: 'Du må velge aktivitetsgrad' },
                            })}
                            error={errors.aktivitetsgrad?.message}
                          >
                            <option value="">Velg hvilken grad</option>
                            {Object.values(AktivitetspliktVurderingType).map((type) => (
                              <option key={type} value={type}>
                                {tekstAktivitetspliktVurderingType[type]}
                              </option>
                            ))}
                          </Select>
                          <div>
                            <Label>Aktivitetsgradsperiode</Label>
                            <BodyLong>Du trenger ikke legge til en sluttdato hvis den ikke er tilgjengelig</BodyLong>
                          </div>
                          <HStack gap="4">
                            <ControlledDatoVelger name="fom" label="Angi startdato" control={control} />
                            <ControlledDatoVelger
                              name="tom"
                              label="Angi sluttdato"
                              control={control}
                              required={false}
                            />
                          </HStack>
                        </>
                      )}
                    </>
                  )}
                  <Textarea
                    label="Vurdering"
                    {...register('beskrivelse', {
                      required: { value: true, message: 'Du må fylle inn vurdering' },
                    })}
                    error={errors.beskrivelse?.message}
                  />
                </VStack>
              ) : (
                <>
                  {mapFailure(hentet, (error) => (
                    <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved henting av vurdering'}</ApiErrorAlert>
                  ))}

                  {!isPending(hentet) && vurdering && (
                    <AktivitetspliktVurderingVisning vurdering={vurdering} visForm={() => {}} erRedigerbar={false} />
                  )}
                </>
              )}

              <div>
                <Heading size="small" spacing>
                  Opprett informasjonbrev rundt aktivitetsplikt til bruker
                </Heading>
                <BodyLong spacing>
                  Den etterlatte skal informeres om aktivitetskravet som vil tre i kraft 6 måneder etter dødsfallet. Det
                  skal opprettes et manuelt informasjonsbrev som skal bli sendt 3-4 måneder etter dødsfallet.
                </BodyLong>
                <PersonButtonLink
                  variant="primary"
                  fnr={oppgave.fnr || '-'}
                  fane={PersonOversiktFane.BREV}
                  disabled={!oppgave.fnr}
                  size="small"
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  Opprett manuelt brev
                </PersonButtonLink>
              </div>
            </HStack>
            {mapFailure(opprettetUnntak, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av unntak'}</ApiErrorAlert>
            ))}
            {mapFailure(opprettetAktivitetsgrad, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av aktivitetsgrad'}</ApiErrorAlert>
            ))}
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgave'}</ApiErrorAlert>
            ))}
            {mapFailure(hentOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved henting av oppgave'}</ApiErrorAlert>
            ))}
          </Modal.Body>
          <Modal.Footer>
            {kanFerdigstilleOppgave() && (
              <Button
                loading={
                  isPending(ferdigstillOppgaveStatus) ||
                  isPending(opprettetAktivitetsgrad) ||
                  isPending(opprettetUnntak) ||
                  isPending(hentOppgaveStatus)
                }
                variant="primary"
                type="button"
                onClick={handleSubmit(ferdigstill)}
              >
                Ferdigstill oppgave
              </Button>
            )}
            <Button
              loading={
                isPending(ferdigstillOppgaveStatus) || isPending(opprettetAktivitetsgrad) || isPending(opprettetUnntak)
              }
              variant="tertiary"
              onClick={() => setVisModal(false)}
            >
              Avbryt
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  )
}
