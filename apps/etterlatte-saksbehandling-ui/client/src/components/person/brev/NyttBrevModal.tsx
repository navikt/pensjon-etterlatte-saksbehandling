import { Button, ErrorMessage, Heading, HStack, Modal, Radio, Select, TextField, VStack } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { Control, Controller, useForm } from 'react-hook-form'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

const NasjonalEllerUtlandRadio = ({ control }: { control: Control<FilledFormData, any> }) => (
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
)

const RedusertEtterInntektRadio = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledRadioGruppe
    name="redusertEtterInntekt"
    control={control}
    legend="Er stønaden redusert etter inntekt?"
    errorVedTomInput="Du må velge om stønaden redusert etter inntekt"
    radios={
      <>
        <Radio value={JaNei.JA}>Ja</Radio>
        <Radio value={JaNei.NEI}>Nei</Radio>
      </>
    }
  />
)

const BorINorgeEllerIkkeAvtaleland = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledRadioGruppe
    name="borINorgeEllerIkkeAvtaleland"
    control={control}
    legend="Bor brukeren i Norge eller i ikke-avtaleland?"
    description="Dette gjelder også EØS/avtale-land der søknad skal behandles uten å mottas fra utenlandske trygdemyndigheter."
    errorVedTomInput="Du må velge om bruker er bosatt i Norge eller i ikke-avtaleland"
    radios={
      <>
        <Radio value={JaNei.JA}>Ja</Radio>
        <Radio value={JaNei.NEI}>Nei</Radio>
      </>
    }
  />
)

const ErOver18Aar = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledRadioGruppe
    name="erOver18Aar"
    control={control}
    legend="Er bruker over 18 år?"
    errorVedTomInput="Du må velge om bruker er over 18 år"
    radios={
      <>
        <Radio value={JaNei.JA}>Ja</Radio>
        <Radio value={JaNei.NEI}>Nei</Radio>
      </>
    }
  />
)

const SoeknadMottattDato = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledDatoVelger
    name="mottattDato"
    label="Når ble søknaden mottatt?"
    control={control}
    errorVedTomInput="Du må velge når søknaden ble mottatt"
  />
)

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
      navigate(`/person/sak/${brev.sakId}/brev/${brev.id}`)
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
                  <>
                    <option value={FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND}>
                      Informasjon om aktivitetsplikt ved 4 måneder
                    </option>
                    <option value={FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND}>
                      Informasjon om aktivitetsplikt ved 6 måneder
                    </option>
                    <option value={FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD}>
                      Informasjon om dødsfall
                    </option>
                    <option value={FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD}>
                      Kvitteringsbrev på mottatt søknad
                    </option>
                    <option value={FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER}>
                      Innhenting av opplysninger
                    </option>
                  </>
                )}
                {sakType === SakType.BARNEPENSJON && (
                  <>
                    <option value={FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD}>Informasjon om dødsfall</option>
                    <option value={FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD}>
                      Kvitteringsbrev på mottatt søknad
                    </option>
                    <option value={FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER}>
                      Innhenting av opplysninger
                    </option>
                  </>
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
                    name="utbetaling"
                    control={control}
                    legend="Kommer stønaden til utbetaling?"
                    errorVedTomInput="Du må velge om stønaden kommer til utbetaling"
                    radios={
                      <>
                        <Radio value={JaNei.JA}>Ja</Radio>
                        <Radio value={JaNei.NEI}>Nei</Radio>
                      </>
                    }
                  />
                  <RedusertEtterInntektRadio control={control} />
                  <NasjonalEllerUtlandRadio control={control} />
                </>
              )}
              {FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND === skjemaet.type && (
                <>
                  <RedusertEtterInntektRadio control={control} />
                  <NasjonalEllerUtlandRadio control={control} />
                </>
              )}
              {[
                FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD,
                FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD,
              ].includes(skjemaet.type) && (
                <>
                  <NasjonalEllerUtlandRadio control={control} />
                  <Controller
                    rules={{
                      required: true,
                      minLength: 1,
                    }}
                    name="avdoedNavn"
                    control={control}
                    render={({ field, fieldState }) => {
                      const { value, ...rest } = field
                      return (
                        <>
                          <TextField label="Avdødes navn" value={value ?? ''} {...rest} />
                          {fieldState.error && <ErrorMessage>Du må oppgi navnet på avdøde.</ErrorMessage>}
                        </>
                      )
                    }}
                  />
                  {FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD === skjemaet.type && (
                    <ErOver18Aar control={control} />
                  )}
                </>
              )}
              {skjemaet.type === FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD && (
                <>
                  <SoeknadMottattDato control={control} />
                  <ErOver18Aar control={control} />
                  <NasjonalEllerUtlandRadio control={control} />
                  <BorINorgeEllerIkkeAvtaleland control={control} />
                </>
              )}
              {skjemaet.type === FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER && (
                <>
                  <ErOver18Aar control={control} />
                  <NasjonalEllerUtlandRadio control={control} />
                </>
              )}
              {skjemaet.type === FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD && (
                <>
                  <SoeknadMottattDato control={control} />
                  <BorINorgeEllerIkkeAvtaleland control={control} />
                </>
              )}
              {skjemaet.type === FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER && (
                <NasjonalEllerUtlandRadio control={control} />
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
      type: FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND
      redusertEtterInntekt: boolean
      nasjonalEllerUtland: NasjonalEllerUtland
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD
      bosattUtland: boolean
      avdoedNavn: string
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD
      mottattDato: Date
      borINorgeEllerIkkeAvtaleland: boolean
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER
      borIUtlandet: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD
      bosattUtland: boolean
      avdoedNavn: string
      erOver18Aar: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD
      mottattDato: Date
      bosattUtland: boolean
      erOver18aar: boolean
      borINorgeEllerIkkeAvtaleland: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER
      borIUtlandet: boolean
      erOver18aar: boolean
    }
  | {
      type: FormType.TOMT_BREV
    }

type FilledFormData = {
  type: FormType
  aktivitetsgrad?: string
  utbetaling?: JaNei | ''
  redusertEtterInntekt?: JaNei | ''
  nasjonalEllerUtland?: NasjonalEllerUtland
  avdoedNavn?: string
  erOver18Aar?: JaNei | ''
  mottattDato?: Date
  borINorgeEllerIkkeAvtaleland?: JaNei
}

enum NasjonalEllerUtland {
  NASJONAL = 'NASJONAL',
  UTLAND = 'UTLAND',
}

enum FormType {
  TOMT_BREV = 'TOMT_BREV',
  OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND = 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND',
  OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND = 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND',
  OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD = 'OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD',
  OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD = 'OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD',
  OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER = 'OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER',
  BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD = 'BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD',
  BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD = 'BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD',
  BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER = 'BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER',
}

function mapFormdataToBrevParametre(formdata: FilledFormData): BrevParametre {
  switch (formdata.type) {
    case FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND:
      return {
        type: formdata.type,
        aktivitetsgrad: formdata.aktivitetsgrad!!,
        utbetaling: formdata.utbetaling!! === JaNei.JA,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === JaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
      }
    case FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND:
      return {
        type: formdata.type,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === JaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD:
      return {
        type: formdata.type,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        avdoedNavn: formdata.avdoedNavn!!,
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD:
      return {
        type: formdata.type,
        mottattDato: formdata.mottattDato!!,
        borINorgeEllerIkkeAvtaleland: formdata.borINorgeEllerIkkeAvtaleland === JaNei.JA,
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER:
      return {
        type: formdata.type,
        borIUtlandet: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
      }
    case FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD:
      return {
        type: formdata.type,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        avdoedNavn: formdata.avdoedNavn!!,
        erOver18Aar: formdata.erOver18Aar === JaNei.JA,
      }
    case FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD:
      return {
        type: formdata.type,
        mottattDato: formdata.mottattDato!!,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        erOver18aar: formdata.erOver18Aar === JaNei.JA,
        borINorgeEllerIkkeAvtaleland: formdata.borINorgeEllerIkkeAvtaleland === JaNei.JA,
      }
    case FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER:
      return {
        type: formdata.type,
        borIUtlandet: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        erOver18aar: formdata.erOver18Aar === JaNei.JA,
      }
    case FormType.TOMT_BREV:
      return {
        type: formdata.type,
      }
    default:
      throw new Error('Valgt type er ikke gyldig')
  }
}
