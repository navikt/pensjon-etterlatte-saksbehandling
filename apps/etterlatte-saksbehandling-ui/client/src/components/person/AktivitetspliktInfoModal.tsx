import {
  BodyLong,
  BodyShort,
  Button,
  Detail,
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
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import {
  hentAktivitspliktVurdering,
  opprettAktivitspliktAktivitetsgrad,
  opprettAktivitspliktUnntak,
} from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { formaterDato } from '~utils/formatering/dato'
import { Toast } from '~shared/alerts/Toast'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { JaNei } from '~shared/types/ISvar'

interface AktivitetspliktVurderingValues {
  aktivitetsplikt: JaNei | null
  aktivitetsgrad: AktivitetspliktVurderingType | ''
  unntak: JaNei | null
  midlertidigUnntak: AktivitetspliktUnntakType | ''
  sluttdato?: Date | null
  beskrivelse: string
}

const AktivitetspliktVurderingValuesDefault: AktivitetspliktVurderingValues = {
  aktivitetsplikt: null,
  aktivitetsgrad: '',
  unntak: null,
  midlertidigUnntak: '',
  sluttdato: undefined,
  beskrivelse: '',
}

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
  const [hentet, hent] = useApiCall(hentAktivitspliktVurdering)
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
            tom:
              data.sluttdato && data.aktivitetsplikt === JaNei.JA ? new Date(data.sluttdato).toISOString() : undefined,
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
            fom: new Date().toISOString(),
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
                          <ControlledDatoVelger
                            name="sluttdato"
                            label="Angi sluttdato for unntaksperiode"
                            description="Du trenger ikke legge til en sluttdato hvis den ikke er tilgjengelig"
                            control={control}
                            required={false}
                          />
                        </>
                      )}
                      {harUnntak === JaNei.NEI && (
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
                    <VStack gap="4">
                      {vurdering.unntak && (
                        <>
                          <Label>Unntak</Label>
                          <BodyShort>{tekstAktivitetspliktUnntakType[vurdering.unntak.unntak]}</BodyShort>

                          {vurdering.unntak.tom && (
                            <>
                              <Label>Sluttdato</Label>
                              <BodyShort>{vurdering.unntak.tom}</BodyShort>
                            </>
                          )}

                          <Label>Vurdering</Label>
                          <BodyShort>{vurdering.unntak.beskrivelse}</BodyShort>

                          <Detail>
                            Vurdering ble utført {formaterDato(vurdering.unntak.opprettet.tidspunkt)} av saksbehandler{' '}
                            {vurdering.unntak.opprettet.ident}
                          </Detail>
                        </>
                      )}
                      {vurdering.aktivitet && (
                        <>
                          <Label>Aktivitetsgrad</Label>
                          <BodyShort>{tekstAktivitetspliktVurderingType[vurdering.aktivitet.aktivitetsgrad]}</BodyShort>

                          <Label>Vurdering</Label>
                          <BodyShort>{vurdering.aktivitet.beskrivelse}</BodyShort>

                          <Detail>
                            Vurdering ble utført {formaterDato(vurdering.aktivitet.opprettet.tidspunkt)} av
                            saksbehandler {vurdering.aktivitet.opprettet.ident}
                          </Detail>
                        </>
                      )}
                    </VStack>
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
                <Button
                  variant="primary"
                  size="small"
                  as="a"
                  href={`/person/${oppgave.id?.toString()}?fane=BREV`}
                  target="_blank"
                >
                  Opprett manuelt brev
                </Button>
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
