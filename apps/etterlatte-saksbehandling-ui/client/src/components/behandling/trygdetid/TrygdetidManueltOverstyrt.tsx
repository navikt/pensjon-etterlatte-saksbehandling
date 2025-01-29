import React from 'react'
import { Alert, Box, Button, Checkbox, Heading, HStack, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IDetaljertBeregnetTrygdetid,
  ITrygdetid,
  oppdaterTrygdetidOverstyrtMigrering,
  opprettTrygdetider,
} from '~shared/api/trygdetid'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Toast } from '~shared/alerts/Toast'
import { useBehandling } from '~components/behandling/useBehandling'
import { FormProvider, useForm } from 'react-hook-form'

export const TrygdetidManueltOverstyrt = ({
  trygdetidId,
  ident,
  beregnetTrygdetid,
  oppdaterTrygdetid,
  tidligereFamiliepleier,
  redigerbar,
}: {
  trygdetidId: string
  ident: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  tidligereFamiliepleier?: boolean
  redigerbar: boolean
}) => {
  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()

  const [oppdaterStatus, oppdaterTrygdetidRequest] = useApiCall(oppdaterTrygdetidOverstyrtMigrering)
  const [opprettStatus, opprettTrygdetid] = useApiCall(opprettTrygdetider)

  const methods = useForm<{
    skalHaProrata: boolean
    anvendtTrygdetid: number
    prorataTeller: number | undefined
    prorataNevner: number | undefined
    begrunnelse: string
  }>({
    defaultValues: {
      skalHaProrata: !!beregnetTrygdetid.resultat.prorataBroek,
      anvendtTrygdetid: !!beregnetTrygdetid.resultat.prorataBroek
        ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
        : beregnetTrygdetid.resultat.samletTrygdetidNorge,
      prorataTeller: beregnetTrygdetid.resultat.prorataBroek?.teller,
      prorataNevner: beregnetTrygdetid.resultat.prorataBroek?.nevner,
      begrunnelse: '',
    },
  })
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    getValues,
  } = methods

  const lagre = () => {
    oppdaterTrygdetidRequest(
      {
        behandlingId: behandling!!.id,
        trygdetidId: trygdetidId,
        anvendtTrygdetid: getValues().anvendtTrygdetid,
        prorataBroek: getValues().skalHaProrata
          ? {
              teller: getValues().prorataTeller!!,
              nevner: getValues().prorataNevner!!,
            }
          : undefined,
      },
      (trygdetid) => {
        oppdaterTrygdetid(trygdetid)
      }
    )
  }

  const opprettNyTrygdetid = () => {
    opprettTrygdetid({ behandlingId: behandling!!.id, overskriv: true }, () => window.location.reload())
  }

  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  const identErIGrunnlag = personopplysninger?.avdoede?.find((person) => person.opplysning.foedselsnummer === ident)
  if (!identErIGrunnlag && !tidligereFamiliepleier) {
    if (ident !== 'UKJENT_AVDOED') {
      return <Alert variant="error">Fant ikke avdød ident {ident} (trygdetid) i behandlingsgrunnlaget</Alert>
    }
  }

  return (
    <>
      <Heading size="small" level="3">
        Manuelt overstyrt trygdetid
      </Heading>

      <FormProvider {...methods}>
        {ident == 'UKJENT_AVDOED' && (
          <Box maxWidth="40rem">
            <VStack gap="3">
              <Alert variant="warning">
                Trygdetiden er koblet til en ukjent avdød. Hvis avdøde i saken er kjent, og familieoversikten er
                oppdatert, bør trygdetid opprettes på nytt. Dette for å unngå å bruke manuelt overstyrt trygdetid der
                dette ikke er nødvendig.
              </Alert>
              <Box maxWidth="20rem">
                <Button variant="danger" size="small" onClick={opprettNyTrygdetid} loading={isPending(opprettStatus)}>
                  Opprett ny trygdetid
                </Button>
              </Box>
            </VStack>
          </Box>
        )}
        <VStack gap="4">
          <TextField
            {...register('anvendtTrygdetid', {
              pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
              required: { value: true, message: 'Må fylles ut' },
            })}
            label="Anvendt trygdetid"
            htmlSize={20}
            error={errors.anvendtTrygdetid?.message}
            readOnly={!redigerbar}
          />

          <Checkbox {...register('skalHaProrata')} readOnly={!redigerbar}>
            Prorata brøk
          </Checkbox>

          {watch('skalHaProrata') && (
            <HStack gap="4">
              <Box width="20rem">
                <TextField
                  {...register('prorataTeller', {
                    pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                    maxLength: {
                      value: 11,
                      message: 'Beløp kan ikke ha flere enn 11 siffer',
                    },
                    required: { value: true, message: 'Må fylles ut' },
                  })}
                  label="Prorata teller"
                  error={errors.prorataTeller?.message}
                  readOnly={!redigerbar}
                />
              </Box>
              <Box width="20rem">
                <TextField
                  {...register('prorataNevner', {
                    pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                    maxLength: {
                      value: 11,
                      message: 'Beløp kan ikke ha flere enn 11 siffer',
                    },
                    required: { value: true, message: 'Må fylles ut' },
                  })}
                  label="Prorata nevner"
                  error={errors.prorataNevner?.message}
                  readOnly={!redigerbar}
                />
              </Box>
            </HStack>
          )}
          <HStack gap="4">
            <Box width="20rem">
              <Textarea
                {...register('begrunnelse', {})}
                label="Begrunnelse"
                error={errors.begrunnelse?.message}
                readOnly={!redigerbar}
              />
            </Box>
          </HStack>
          {redigerbar && (
            <Box width="20rem">
              <Button variant="primary" size="small" onClick={handleSubmit(lagre)} loading={isPending(oppdaterStatus)}>
                Lagre overstyrt trygdetid
              </Button>
            </Box>
          )}
        </VStack>
        {mapResult(oppdaterStatus, {
          pending: <Spinner label="Lagrer trygdetid" />,
          error: () => <ApiErrorAlert>En feil har oppstått ved lagring av trygdetid</ApiErrorAlert>,
          success: () => <Toast melding="Trygdetid lagret" position="bottom-center" />,
        })}
        {mapResult(opprettStatus, {
          pending: <Spinner label="Overstyrer trygdetid" />,
          error: () => <ApiErrorAlert>En feil har oppstått ved overstyring av trygdetid</ApiErrorAlert>,
          success: () => <Toast melding="Trygdetid overstyrt" position="bottom-center" />,
        })}
      </FormProvider>
    </>
  )
}
