import React from 'react'
import { Box, Button, Checkbox, HStack, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IDetaljertBeregnetTrygdetid, ITrygdetid, oppdaterTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Toast } from '~shared/alerts/Toast'
import { useBehandling } from '~components/behandling/useBehandling'
import { FormProvider, useForm } from 'react-hook-form'

interface IOverstyrtTrygdetidForm {
  skalHaProrata: boolean
  anvendtTrygdetid: number
  prorataTeller: number | undefined
  prorataNevner: number | undefined
  begrunnelse: string
}

export const TrygdetidManueltOverstyrtSkjema = ({
  trygdetidId,
  beregnetTrygdetid,
  oppdaterTrygdetid,
  setVisSkjema,
}: {
  trygdetidId: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const behandling = useBehandling()
  const [oppdaterStatus, oppdaterTrygdetidRequest] = useApiCall(oppdaterTrygdetidOverstyrtMigrering)
  const harProrata = !!beregnetTrygdetid.resultat.prorataBroek
  const methods = useForm<IOverstyrtTrygdetidForm>({
    defaultValues: {
      skalHaProrata: harProrata,
      anvendtTrygdetid: harProrata
        ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
        : beregnetTrygdetid.resultat.samletTrygdetidNorge,
      prorataTeller: beregnetTrygdetid.resultat.prorataBroek?.teller,
      prorataNevner: beregnetTrygdetid.resultat.prorataBroek?.nevner,
      begrunnelse: beregnetTrygdetid.resultat.overstyrtBegrunnelse,
    },
  })
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = methods

  const lagre = (data: IOverstyrtTrygdetidForm) => {
    oppdaterTrygdetidRequest(
      {
        behandlingId: behandling!!.id,
        trygdetidId: trygdetidId,
        anvendtTrygdetid: data.anvendtTrygdetid,
        prorataBroek: data.skalHaProrata
          ? {
              teller: data.prorataTeller!!,
              nevner: data.prorataNevner!!,
            }
          : undefined,
        begrunnelse: data.begrunnelse,
      },
      (trygdetid) => {
        oppdaterTrygdetid(trygdetid)
        setVisSkjema(false)
      }
    )
  }

  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(lagre)}>
        <VStack gap="space-4">
          <TextField
            {...register('anvendtTrygdetid', {
              pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
              max: { value: 40, message: 'Kan ikke være høyere enn 40 år' },
              min: { value: 0, message: 'Kan ikke være under 0 år' },
              required: { value: true, message: 'Må fylles ut' },
            })}
            label="Anvendt trygdetid (år)"
            htmlSize={20}
            error={errors.anvendtTrygdetid?.message}
          />

          <Checkbox {...register('skalHaProrata')}>Prorata brøk</Checkbox>

          {watch('skalHaProrata') && (
            <HStack gap="space-4">
              <Box width="10rem">
                <TextField
                  {...register('prorataTeller', {
                    pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                    maxLength: {
                      value: 4,
                      message: 'Beløp kan ikke ha flere enn 11 siffer',
                    },
                    required: { value: true, message: 'Må fylles ut' },
                  })}
                  label="Prorata teller (måneder)"
                  error={errors.prorataTeller?.message}
                />
              </Box>
              <Box width="10rem">
                <TextField
                  {...register('prorataNevner', {
                    pattern: { value: /^[0-9]+$/, message: 'Kun tall' },
                    maxLength: {
                      value: 4,
                      message: 'Beløp kan ikke ha flere enn 11 siffer',
                    },
                    required: { value: true, message: 'Må fylles ut' },
                  })}
                  label="Prorata nevner (måneder)"
                  error={errors.prorataNevner?.message}
                />
              </Box>
            </HStack>
          )}
          <Box width="35rem">
            <Textarea
              {...register('begrunnelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Begrunnelse"
              error={errors.begrunnelse?.message}
            />
          </Box>
          <VStack gap="space-4">
            {mapResult(oppdaterStatus, {
              pending: <Spinner label="Lagrer overstyrt trygdetid" />,
              error: () => <ApiErrorAlert>En feil har oppstått ved lagring av overstyrt trygdetid</ApiErrorAlert>,
              success: () => <Toast melding="Overstyrt trygdetid lagret" position="bottom-center" />,
            })}
            <Box width="20rem">
              <Button variant="primary" size="small" type="submit" loading={isPending(oppdaterStatus)}>
                Lagre
              </Button>
            </Box>
          </VStack>
        </VStack>
      </form>
    </FormProvider>
  )
}
