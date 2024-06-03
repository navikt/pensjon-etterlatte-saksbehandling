import { Button, Heading, HStack, Modal, Radio, Select, VStack } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { useForm } from 'react-hook-form'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { IValgJaNei } from '~shared/types/Aktivitetsplikt'
import { SakType } from '~shared/types/sak'

export const NyttBrevModal = ({ sakId, sakType }: { sakId: number; sakType: SakType }) => {
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettBrevAvSpesifikkTypeForSak)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const defaultData: FilledFormData = {
    type: FormType.TOMT_BREV,
    utbetaling: '',
  }

  const {
    formState: { errors },
    handleSubmit,
    watch,
    register,
    control,
  } = useForm({ defaultValues: defaultData })

  const skjemaet = watch()

  const opprettBrev = (formData: FilledFormData) => {
    const brevParametre = mapFormdataToBrevParametre(formData)

    opprettBrevApiCall({ sakId: sakId, body: brevParametre }, (brev) => {
      setOpen(false)
      navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
    })
  }

  const avbryt = () => {
    setOpen(false)
  }

  return (
    <>
      <Button variant="primary" icon={<DocPencilIcon />} iconPosition="right" onClick={() => setOpen(true)}>
        Nytt brev
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Lag nytt brev
          </Heading>
        </Modal.Header>

        <form onSubmit={handleSubmit(opprettBrev)}>
          <Modal.Body>
            <VStack gap="4">
              <Select
                error={errors?.type?.message}
                label="Type"
                {...register('type', {
                  required: { value: true, message: 'Feltet er påkrevd' },
                })}
              >
                <option value={FormType.TOMT_BREV}>Manuelt brev</option>
                {sakType === SakType.OMSTILLINGSSTOENAD && (
                  <option value={FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND}>
                    Informasjon om aktivitetsplikt ved 4 måneder
                  </option>
                )}
              </Select>

              {skjemaet.type === FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND && (
                <>
                  <Select
                    error={errors?.aktivitetsgrad?.message}
                    label="Aktivitetsgrad"
                    {...register('aktivitetsgrad', {
                      required: { value: true, message: 'Du må velge aktivitetsgrad' },
                      validate: { notDefault: (value) => !!value },
                    })}
                  >
                    <option value="">Velg aktivitetsgrad</option>
                    <option value="IKKE_I_AKTIVITET">Ikke i aktivitet</option>
                    <option value="UNDER_50_PROSENT">Under 50%</option>
                    <option value="OVER_50_PROSENT">Over 50%</option>
                  </Select>
                  <ControlledRadioGruppe
                    name="nasjonalEllerUtland"
                    control={control}
                    legend="Er bruker bosatt i Norge eller utlandet?"
                    errorVedTomInput="Du må velge om bruker er bosatt i Norge eller utlandet"
                    radios={
                      <>
                        <Radio value={NasjonalEllerUtland.NASJONAL}>Norge</Radio>
                        <Radio value={NasjonalEllerUtland.UTLAND}>Utlandet</Radio>
                      </>
                    }
                  />
                  <ControlledRadioGruppe
                    name="redusertEtterInntekt"
                    control={control}
                    legend="Er stønaden redusert etter inntekt?"
                    errorVedTomInput="Du må velge om stønaden redusert etter inntekt"
                    radios={
                      <>
                        <Radio value={IValgJaNei.JA}>Ja</Radio>
                        <Radio value={IValgJaNei.NEI}>Nei</Radio>
                      </>
                    }
                  />
                  <ControlledRadioGruppe
                    name="utbetaling"
                    control={control}
                    legend="Kommer stønaden til utbetaling?"
                    errorVedTomInput="Du må velge om stønaden kommer til utbetaling"
                    radios={
                      <>
                        <Radio value={IValgJaNei.JA}>Ja</Radio>
                        <Radio value={IValgJaNei.NEI}>Nei</Radio>
                      </>
                    }
                  />
                </>
              )}
            </VStack>
          </Modal.Body>

          <Modal.Footer>
            <HStack gap="4" justify="end">
              <Button variant="secondary" type="button" disabled={isPending(opprettBrevStatus)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(opprettBrevStatus)}>
                Opprett brev
              </Button>
            </HStack>
          </Modal.Footer>
        </form>
        {mapFailure(opprettBrevStatus, (error) => (
          <ApiErrorAlert>{error.detail || 'Ukjent feil oppsto ved oppretting av brev'}</ApiErrorAlert>
        ))}
      </Modal>
    </>
  )
}

export type BrevParametre =
  | {
      type: FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND
      aktivitetsgrad: string
      utbetaling: boolean
      redusertEtterInntekt: boolean
      nasjonalEllerUtland: NasjonalEllerUtland
    }
  | {
      type: FormType.TOMT_BREV
    }

type FilledFormData = {
  type: FormType
  aktivitetsgrad?: string
  utbetaling?: IValgJaNei | ''
  redusertEtterInntekt?: IValgJaNei | ''
  nasjonalEllerUtland?: NasjonalEllerUtland
}

enum NasjonalEllerUtland {
  NASJONAL = 'NASJONAL',
  UTLAND = 'UTLAND',
}

enum FormType {
  TOMT_BREV = 'TOMT_BREV',
  OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND = 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND',
}

function mapFormdataToBrevParametre(formdata: FilledFormData): BrevParametre {
  switch (formdata.type) {
    case FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND:
      return {
        type: formdata.type,
        aktivitetsgrad: formdata.aktivitetsgrad!!,
        utbetaling: formdata.utbetaling!! === IValgJaNei.JA,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === IValgJaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
      }
    case FormType.TOMT_BREV:
      return {
        type: formdata.type,
      }
    default:
      throw new Error('Valgt type er ikke gyldig')
  }
}
