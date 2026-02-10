import {
  BodyShort,
  Button,
  ErrorMessage,
  Heading,
  HStack,
  Modal,
  Radio,
  Select,
  TextField,
  VStack,
} from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { Control, Controller, useForm } from 'react-hook-form'
import { isPending, mapFailure, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { Spraak } from '~shared/types/Brev'
import { formaterSpraak } from '~utils/formatering/formatering'
import { ClickEvent, trackClick } from '~utils/analytics'
import { hentGjeldendeGrunnbeloep } from '~shared/api/beregning'
import Spinner from '~shared/Spinner'

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

const VedtakDato = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledDatoVelger
    name="datoForVedtak"
    label="Når ble vedtaket gjort?"
    control={control}
    errorVedTomInput="Du må velge dato for vedtaket"
  />
)

const KlageMotattDato = ({ control }: { control: Control<FilledFormData, any> }) => (
  <ControlledDatoVelger
    name="datoMottatKlage"
    label="Når ble klagen mottatt?"
    control={control}
    errorVedTomInput="Du må velge når klagen ble mottatt"
  />
)

export const NyttBrevModal = ({
  sakId,
  sakType,
  modalButtonlabel = 'Nytt brev',
  modalButtonVariant = 'primary',
}: {
  sakId: number
  sakType: SakType
  modalButtonlabel?: string
  modalButtonVariant?: 'primary' | 'secondary'
}) => {
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettBrevAvSpesifikkTypeForSak)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const [hentGjeldeneGrunnbeloepResult, hentGjeldendeGrunnbeloepFetch] = useApiCall(hentGjeldendeGrunnbeloep)

  useEffect(() => {
    hentGjeldendeGrunnbeloepFetch({})
  }, [])

  const grunnbeloep = mapSuccess(hentGjeldeneGrunnbeloepResult, (data) => data.grunnbeløp)

  const defaultData: FilledFormData = {
    type: FormType.TOMT_BREV,
    spraak: Spraak.NB,
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
    const brevParametre = mapFormdataToBrevParametre(formData, sakType, grunnbeloep)

    trackClick(ClickEvent.OPPRETT_NYTT_BREV)

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
      <Button
        variant={modalButtonVariant}
        icon={<DocPencilIcon aria-hidden />}
        iconPosition="right"
        onClick={() => setOpen(true)}
      >
        {modalButtonlabel}
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Lag nytt brev
          </Heading>
        </Modal.Header>

        <form onSubmit={handleSubmit(opprettBrev)}>
          <Modal.Body>
            <VStack gap="space-4">
              <Select
                error={errors?.type?.message}
                label="Type"
                {...register('type', {
                  required: { value: true, message: 'Feltet er påkrevd' },
                })}
              >
                <option value={FormType.TOMT_BREV}>Manuelt brev</option>
                <option value={FormType.KLAGE_SAKSBEHANDLINGSTID}>Klage saksbehandlingstid informasjon</option>
                {sakType === SakType.OMSTILLINGSSTOENAD && (
                  <>
                    <option value={FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND}>
                      Informasjon ved 6 måneder - varig unntak for aktivitetsplikt
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
              <Select
                {...register('spraak', {
                  required: {
                    value: true,
                    message: 'Du må velge ',
                  },
                })}
                label="Språk/målform"
                error={errors.spraak?.message}
                defaultValue={defaultData.spraak}
              >
                {Object.values(Spraak).map((spraak) => (
                  <option key={spraak} value={spraak}>
                    {formaterSpraak(spraak)}
                  </option>
                ))}
              </Select>
              {skjemaet.type === FormType.KLAGE_SAKSBEHANDLINGSTID && (
                <>
                  <NasjonalEllerUtlandRadio control={control} />
                  <VedtakDato control={control} />
                  <KlageMotattDato control={control} />
                </>
              )}
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
                  {mapResult(hentGjeldeneGrunnbeloepResult, {
                    pending: <Spinner label="Henter gjeldende grunnbeløp" />,
                    success: (data) => (
                      <BodyShort>
                        <strong>Gjeldende grunnbeløp:</strong> {data.grunnbeløp} kroner
                      </BodyShort>
                    ),
                    error: (error) => (
                      <ApiErrorAlert>Kunne ikke hente gjeldende grunnbeløp: {error.detail}</ApiErrorAlert>
                    ),
                  })}
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
            <HStack gap="space-4" justify="end">
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
      spraak: Spraak
      aktivitetsgrad: string
      utbetaling: boolean
      redusertEtterInntekt: boolean
      nasjonalEllerUtland: NasjonalEllerUtland
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND
      spraak: Spraak
      redusertEtterInntekt: boolean
      nasjonalEllerUtland: NasjonalEllerUtland
      halvtGrunnbeloep: number
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD
      spraak: Spraak
      bosattUtland: boolean
      avdoedNavn: string
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD
      spraak: Spraak
      mottattDato: Date
      borINorgeEllerIkkeAvtaleland: boolean
    }
  | {
      type: FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER
      spraak: Spraak
      borIUtlandet: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD
      spraak: Spraak
      bosattUtland: boolean
      avdoedNavn: string
      erOver18Aar: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD
      spraak: Spraak
      mottattDato: Date
      bosattUtland: boolean
      erOver18aar: boolean
      borINorgeEllerIkkeAvtaleland: boolean
    }
  | {
      type: FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER
      spraak: Spraak
      borIUtlandet: boolean
      erOver18aar: boolean
    }
  | {
      type: FormType.KLAGE_SAKSBEHANDLINGSTID
      spraak: Spraak
      datoMottatKlage: Date
      datoForVedtak: Date
      borIUtlandet: boolean
      sakType: SakType
    }
  | {
      type: FormType.TOMT_BREV
      spraak: Spraak
    }

type FilledFormData = {
  type: FormType
  spraak: Spraak
  aktivitetsgrad?: string
  utbetaling?: JaNei | ''
  redusertEtterInntekt?: JaNei | ''
  nasjonalEllerUtland?: NasjonalEllerUtland
  avdoedNavn?: string
  erOver18Aar?: JaNei | ''
  mottattDato?: Date
  borINorgeEllerIkkeAvtaleland?: JaNei
  datoForVedtak?: Date
  datoMottatKlage?: Date
}

export enum NasjonalEllerUtland {
  NASJONAL = 'NASJONAL',
  UTLAND = 'UTLAND',
}

enum FormType {
  TOMT_BREV = 'TOMT_BREV',
  KLAGE_SAKSBEHANDLINGSTID = 'KLAGE_SAKSBEHANDLINGSTID',
  OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND = 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND',
  OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND = 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND',
  OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD = 'OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD',
  OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD = 'OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD',
  OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER = 'OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER',
  BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD = 'BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD',
  BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD = 'BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD',
  BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER = 'BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER',
}

function mapFormdataToBrevParametre(
  formdata: FilledFormData,
  sakType: SakType,
  grunnbeloep: number | null
): BrevParametre {
  switch (formdata.type) {
    case FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        aktivitetsgrad: formdata.aktivitetsgrad!!,
        utbetaling: formdata.utbetaling!! === JaNei.JA,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === JaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
      }
    case FormType.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        redusertEtterInntekt: formdata.redusertEtterInntekt!! === JaNei.JA,
        nasjonalEllerUtland: formdata.nasjonalEllerUtland!!,
        halvtGrunnbeloep: Math.floor(grunnbeloep!! / 2),
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        avdoedNavn: formdata.avdoedNavn!!,
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        mottattDato: formdata.mottattDato!!,
        borINorgeEllerIkkeAvtaleland: formdata.borINorgeEllerIkkeAvtaleland === JaNei.JA,
      }
    case FormType.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        borIUtlandet: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
      }
    case FormType.BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        avdoedNavn: formdata.avdoedNavn!!,
        erOver18Aar: formdata.erOver18Aar === JaNei.JA,
      }
    case FormType.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        mottattDato: formdata.mottattDato!!,
        bosattUtland: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        erOver18aar: formdata.erOver18Aar === JaNei.JA,
        borINorgeEllerIkkeAvtaleland: formdata.borINorgeEllerIkkeAvtaleland === JaNei.JA,
      }
    case FormType.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        borIUtlandet: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
        erOver18aar: formdata.erOver18Aar === JaNei.JA,
      }
    case FormType.KLAGE_SAKSBEHANDLINGSTID:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
        sakType: sakType,
        datoForVedtak: formdata.datoForVedtak!!,
        datoMottatKlage: formdata.datoMottatKlage!!,
        borIUtlandet: formdata.nasjonalEllerUtland === NasjonalEllerUtland.UTLAND,
      }
    case FormType.TOMT_BREV:
      return {
        type: formdata.type,
        spraak: formdata.spraak,
      }
    default:
      throw new Error('Valgt type er ikke gyldig')
  }
}
