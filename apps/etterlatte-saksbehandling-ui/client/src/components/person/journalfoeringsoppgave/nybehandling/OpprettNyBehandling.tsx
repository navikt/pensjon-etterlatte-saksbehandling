import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype, formaterSpraak, mapRHFArrayToStringArray } from '~utils/formatering/formatering'
import { Alert, Box, Button, Heading, HStack, Select, VStack } from '@navikt/ds-react'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import React, { useEffect } from 'react'
import { Spraak } from '~shared/types/Brev'
import { FormProvider, useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { temaFraSakstype } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

export interface NyBehandlingSkjema {
  sakType: SakType
  spraak: Spraak | null
  mottattDato: string
  persongalleri: {
    soeker: string
    innsender: string | null
    gjenlevende?: Array<{ value: string }>
    avdoed?: Array<{ value: string }>
    soesken?: Array<{ value: string }>
  }
}

export default function OpprettNyBehandling() {
  const { oppgave, nyBehandlingRequest, journalpost, sakMedBehandlinger } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  if (!oppgave || !erOppgaveRedigerbar(oppgave.status)) {
    return <Navigate to="../" relative="path" />
  }

  const harAapneBehandlinger = sakMedBehandlinger?.behandlinger?.some(
    (behandling) =>
      ![
        IBehandlingStatus.ATTESTERT,
        IBehandlingStatus.IVERKSATT,
        IBehandlingStatus.AVSLAG,
        IBehandlingStatus.AVBRUTT,
      ].includes(behandling.status)
  )

  const neste = () => navigate('oppsummering', { relative: 'path' })

  const tilbake = () => navigate('../', { relative: 'path' })

  const mapStringArrayToRHFArray = (stringArray?: string[]): Array<{ value: string }> => {
    return !!stringArray
      ? stringArray.map((value) => {
          return { value }
        })
      : []
  }

  const konverterAvdoedForOmstillingstoenad = (avdoed?: string[]): Array<{ value: string }> => {
    if (avdoed && avdoed[0]) {
      return [{ value: avdoed[0] }]
    } else {
      return [{ value: '' }]
    }
  }

  const mapAvdoed = (sakType?: SakType, avdoed?: string[]) =>
    sakType === SakType.OMSTILLINGSSTOENAD
      ? konverterAvdoedForOmstillingstoenad(avdoed)
      : mapStringArrayToRHFArray(avdoed)

  const methods = useForm<NyBehandlingSkjema>({
    defaultValues: {
      ...nyBehandlingRequest,
      persongalleri: {
        innsender: nyBehandlingRequest?.persongalleri?.innsender || null,
        soeker: nyBehandlingRequest?.persongalleri?.soeker,
        gjenlevende: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.gjenlevende),
        soesken: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.soesken),
        avdoed: mapAvdoed(nyBehandlingRequest?.sakType, nyBehandlingRequest?.persongalleri?.avdoed),
      },
    },
  })

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    setValue,
    watch,
  } = methods

  const onSubmit = (data: NyBehandlingSkjema) => {
    dispatch(
      settNyBehandlingRequest({
        sakType: data.sakType,
        spraak: data.spraak!,
        mottattDato: new Date(data.mottattDato).toISOString(),
        persongalleri: {
          ...data.persongalleri,
          gjenlevende: mapRHFArrayToStringArray(data.persongalleri.gjenlevende),
          avdoed: mapRHFArrayToStringArray(data.persongalleri.avdoed).filter((val) => val !== ''),
          soesken: mapRHFArrayToStringArray(data.persongalleri.soesken),
        },
      })
    )

    neste()
  }

  const valgtSakType = watch('sakType')

  useEffect(() => {
    setValue('persongalleri.avdoed', mapAvdoed(valgtSakType, nyBehandlingRequest?.persongalleri?.avdoed))
  }, [valgtSakType])

  return (
    <FormWrapper $column>
      <FormProvider {...methods}>
        <VStack gap="4">
          <Heading size="medium" spacing>
            Opprett behandling
          </Heading>

          {harAapneBehandlinger && (
            <Alert variant="warning">
              Saken har allerede en åpen behandling. Den må ferdigstilles eller avbrytes før en ny behandling kan
              opprettes.
            </Alert>
          )}

          <Select
            {...register('sakType', {
              required: { value: true, message: 'Saktype må være satt' },
            })}
            label="Hvilken type er saken?"
            error={errors.sakType?.message}
          >
            <option value="" disabled>
              Velg ...
            </option>
            <option value={SakType.BARNEPENSJON}>{formaterSakstype(SakType.BARNEPENSJON)}</option>
            <option value={SakType.OMSTILLINGSSTOENAD}>{formaterSakstype(SakType.OMSTILLINGSSTOENAD)}</option>
          </Select>

          {journalpost?.tema !== temaFraSakstype(valgtSakType) && (
            <Alert variant="warning">Valgt sakstype matcher ikke tema i journalposten. Er dette riktig?</Alert>
          )}

          <Select
            {...register('spraak', {
              required: { value: true, message: 'Du må velge språk/målform for behandlingen' },
            })}
            label="Hva skal språket/målform være?"
            error={errors.spraak?.message}
          >
            <option value="" disabled>
              Velg ...
            </option>
            <option value={Spraak.NB}>{formaterSpraak(Spraak.NB)}</option>
            <option value={Spraak.NN}>{formaterSpraak(Spraak.NN)}</option>
            <option value={Spraak.EN}>{formaterSpraak(Spraak.EN)}</option>
          </Select>

          <ControlledDatoVelger
            name="mottattDato"
            label="Mottatt dato"
            description="Datoen søknaden ble mottatt (husk at denne vises i vedtaksbrev)"
            control={control}
            errorVedTomInput="Du må legge inn datoen søknaden ble mottatt"
          />
          <Box marginBlock="12 0">
            <Heading size="medium" spacing>
              Persongalleri
            </Heading>
          </Box>

          {!valgtSakType && <Alert variant="warning">Du må velge saktype!</Alert>}
          {valgtSakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad />}
          {valgtSakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon />}

          <VStack gap="2">
            <HStack gap="4" justify="center">
              <Button variant="secondary" onClick={tilbake} type="button">
                Tilbake
              </Button>

              <Button variant="primary" type="submit" onClick={handleSubmit(onSubmit)}>
                Neste
              </Button>
            </HStack>
            <HStack justify="center">
              <AvbrytBehandleJournalfoeringOppgave />
            </HStack>
          </VStack>
        </VStack>
      </FormProvider>
    </FormWrapper>
  )
}
