import { Button, Heading, Modal, Radio, Select, VStack } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { useForm } from 'react-hook-form'
import { FlexRow } from '~shared/styled'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { IValgJaNei } from '~shared/types/Aktivitetsplikt'
import { capitalize } from '~utils/formattering'

export const NyttBrevModal = ({ sakId }: { sakId: number }) => {
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettBrevAvSpesifikkTypeForSak)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const defaultData: FilledFormData = {
    type: 'TOMT_BREV',
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
      <Button
        variant="primary"
        icon={<DocPencilIcon />}
        iconPosition="right"
        size="small"
        onClick={() => setOpen(true)}
      >
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
                <option value="OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND">
                  Informasjon om aktivitetsplikt ved 4 måneder
                </option>
                <option value="TOMT_BREV">Manuelt brev</option>
              </Select>

              {skjemaet.type === 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND' && (
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
                    name="utbetaling"
                    control={control}
                    legend="Har bruker utbetaling?"
                    errorVedTomInput="Du må velge om bruker har utbetaling"
                    radios={
                      <>
                        <Radio value={IValgJaNei.JA}>Ja</Radio>
                        <Radio value={IValgJaNei.NEI}>Nei</Radio>
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
                    name="nasjonalEllerUtland"
                    control={control}
                    legend="Er saken nasjonal eller utland?"
                    errorVedTomInput="Du må velge om saken nasjonal eller utland"
                    radios={
                      <>
                        <Radio value={NasjonalEllerUtland.NASJONAL}>{capitalize(NasjonalEllerUtland.NASJONAL)}</Radio>
                        <Radio value={NasjonalEllerUtland.UTLAND}>{capitalize(NasjonalEllerUtland.UTLAND)}</Radio>
                      </>
                    }
                  />
                </>
              )}
            </VStack>
          </Modal.Body>

          <Modal.Footer>
            <FlexRow justify="right">
              <Button variant="secondary" type="button" disabled={isPending(opprettBrevStatus)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(opprettBrevStatus)}>
                Opprett brev
              </Button>
            </FlexRow>
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
      type: 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND'
      aktivitetsgrad: string
      utbetaling: boolean
      redusertEtterInntekt: boolean
      nasjonalEllerUtland: NasjonalEllerUtland
    }
  | {
      type: 'TOMT_BREV'
    }

type FilledFormData = {
  type: string
  aktivitetsgrad?: string
  utbetaling?: IValgJaNei | ''
  redusertEtterInntekt?: IValgJaNei | ''
  nasjonalEllerUtland?: NasjonalEllerUtland
}

enum NasjonalEllerUtland {
  NASJONAL = 'NASJONAL',
  UTLAND = 'UTLAND',
}

function mapFormdataToBrevParametre(formdata: FilledFormData): BrevParametre {
  switch (formdata.type) {
    case 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND':
      return {
        type: formdata.type,
        aktivitetsgrad: formdata.aktivitetsgrad!!,
        utbetaling: formdata.utbetaling!! === IValgJaNei.JA,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === IValgJaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
      }
    case 'TOMT_BREV':
      return {
        type: formdata.type,
      }
    default:
      throw new Error('Valgt type er ikke gyldig')
  }
}
