import {
  BodyLong,
  BodyShort,
  Button,
  Detail,
  Heading,
  HStack,
  Label,
  Modal,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useForm } from 'react-hook-form'
import {
  AktivitetspliktVurderingType,
  IAktivitetspliktVurdering,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { hentAktivitspliktVurdering, opprettAktivitspliktVurdering } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { formaterStringDato } from '~utils/formattering'
import { Toast } from '~shared/alerts/Toast'

interface AktivitetspliktVurderingValues {
  aktivitetsgrad: AktivitetspliktVurderingType | ''
  // unntak: boolean | null
  beskrivelse: string
}

const AktivitetspliktVurderingValuesDefault: AktivitetspliktVurderingValues = {
  aktivitetsgrad: '',
  // unntak: null,
  beskrivelse: '',
}

export const AktivitetspliktInfoModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [visModal, setVisModal] = useState(false)
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurdering>()

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)
  const [opprettet, opprett] = useApiCall(opprettAktivitspliktVurdering)
  const [hentet, hent] = useApiCall(hentAktivitspliktVurdering)

  const {
    register,
    handleSubmit,
    formState: { errors },
    // control,
  } = useForm<AktivitetspliktVurderingValues>({
    defaultValues: AktivitetspliktVurderingValuesDefault,
  })

  const ferdigstill = (data: AktivitetspliktVurderingValues) => {
    opprett(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: undefined,
          vurdering: data.aktivitetsgrad as AktivitetspliktVurderingType,
          // unntak: data.unntak!!,
          fom: new Date().toISOString(),
          beskrivelse: data.beskrivelse,
        },
      },
      () => {
        apiFerdigstillOppgave(oppgave.id, () => {
          setVisModal(false)
        })
      }
    )
  }

  useEffect(() => {
    if (oppgave.status !== Oppgavestatus.UNDER_BEHANDLING) {
      hent({ sakId: oppgave.sakId, oppgaveId: oppgave.id }, (result) => {
        setVurdering(result)
      })
    }
  }, [])

  return (
    <>
      {isSuccess(opprettet) && <Toast melding="Vurdering lagret og oppgave ferdigstilt" />}
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal
          open={visModal}
          onClose={() => setVisModal(false)}
          header={{ label: 'Oppfølging av aktivitetsplikt', heading: 'Send brev og opprett oppgave for oppfølging' }}
        >
          <Modal.Body>
            <HStack gap="12">
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
                  href={`/person/${oppgave.fnr?.toString()}?fane=BREV`}
                  target="_blank"
                >
                  Opprett manuelt brev
                </Button>
              </div>

              {oppgave.status === Oppgavestatus.UNDER_BEHANDLING ? (
                <VStack gap="4">
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
                  {/*
                  <ControlledRadioGruppe
                    name="unntak"
                    control={control}
                    errorVedTomInput="Du må velge om bruker har unntak fra aktivitetsplikt"
                    legend="Er det unntak for bruker?"
                    radios={
                      <>
                        <Radio size="small" value={true}>
                          Ja
                        </Radio>
                        <Radio size="small" value={false}>
                          Nei
                        </Radio>
                      </>
                    }
                  />
                  */}
                  <Textarea
                    label="Beskrivelse"
                    {...register('beskrivelse', {
                      required: { value: true, message: 'Du må fylle inn beskrivelse' },
                    })}
                    error={errors.beskrivelse?.message}
                  />
                </VStack>
              ) : (
                <>
                  <Spinner label="Henter vurdering av aktivitetsplikt" visible={isPending(hentet)} />

                  {mapFailure(hentet, (error) => (
                    <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved henting av vurdering'}</ApiErrorAlert>
                  ))}

                  {!isPending(hentet) && vurdering && (
                    <VStack gap="4">
                      <>
                        <Label>Aktivitetsgrad</Label>
                        <BodyShort>{tekstAktivitetspliktVurderingType[vurdering.vurdering]}</BodyShort>
                      </>

                      {/*
                      <>
                        <Label>Unntak</Label>
                        <BodyShort>{vurdering.unntak ? 'Ja' : 'Nei'}</BodyShort>
                      </>
                      */}
                      <>
                        <Label>Beskrivelse</Label>
                        <BodyShort>{vurdering.beskrivelse}</BodyShort>
                      </>

                      <Detail>
                        Vurdering ble utført {formaterStringDato(vurdering.opprettet.tidspunkt)} av saksbehandler{' '}
                        {vurdering.opprettet.ident}
                      </Detail>
                    </VStack>
                  )}
                </>
              )}
            </HStack>
            {mapFailure(opprettet, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av vurdering'}</ApiErrorAlert>
            ))}
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgave'}</ApiErrorAlert>
            ))}
          </Modal.Body>
          <Modal.Footer>
            {oppgave.status === Oppgavestatus.UNDER_BEHANDLING && (
              <Button
                loading={isPending(ferdigstillOppgaveStatus) || isPending(opprettet)}
                variant="primary"
                type="button"
                onClick={handleSubmit(ferdigstill)}
              >
                Ferdigstill oppgave
              </Button>
            )}
            <Button
              loading={isPending(ferdigstillOppgaveStatus) || isPending(opprettet)}
              variant="secondary"
              onClick={() => setVisModal(false)}
            >
              Lukk modal
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  )
}
