import { OppgaveDTO } from '~shared/types/oppgave'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avsluttOmgjoeringsoppgave } from '~shared/api/klage'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Button, HStack, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForm } from 'react-hook-form'
import { hentBehandlingerISak } from '~shared/api/sak'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

interface AvsluttOmgjoeringSkjema {
  omgjoerendeBehandling?: string
  begrunnelse: string
  hvorforAvsluttes: GrunnForAvslutning
}

export enum GrunnForAvslutning {
  OMGJORT_ALLEREDE = 'OMGJORT_ALLEREDE',
  OMGJOERINGSOPPGAVE_OPPRETTET_VED_FEIL = 'OMGJOERINGSOPPGAVE_OPPRETTET_VED_FEIL',
}

export function AvsluttOmgjoeringsoppgave({ oppgave }: { oppgave: OppgaveDTO }) {
  const [visSkjema, setVisSkjema] = useState(false)

  const [behandlingerISakResult, behandlingerISakFetch] = useApiCall(hentBehandlingerISak)
  const [avsluttOmgjoeringsoppgaveResult, avsluttOmgjoeringsoppgaveFetch, resetResult] =
    useApiCall(avsluttOmgjoeringsoppgave)

  useEffect(() => {
    behandlingerISakFetch({ sakId: oppgave.sakId })
  }, [oppgave.sakId])

  // cleanup
  useEffect(() => {
    resetResult()
  }, [visSkjema])

  const {
    control,
    handleSubmit,
    register,
    watch,
    formState: { errors },
  } = useForm<AvsluttOmgjoeringSkjema>()

  function avsluttOmgjoering(skjemadata: AvsluttOmgjoeringSkjema) {
    avsluttOmgjoeringsoppgaveFetch({
      oppgaveId: oppgave.id,
      ...skjemadata,
    })
  }

  return (
    <>
      <BodyShort>
        Hvis vedtaket allikevel ikke skulle omgjøres, eller det allerede har blitt omgjort i en annen revurdering kan
        denne oppgaven avsluttes.
      </BodyShort>

      {visSkjema ? (
        <form onSubmit={handleSubmit(avsluttOmgjoering)}>
          <VStack gap="4">
            <ControlledRadioGruppe
              name="hvorforAvsluttes"
              control={control}
              legend="Hvorfor skal omgjøringsoppgaven avsluttes?"
              radios={
                <>
                  <Radio value={GrunnForAvslutning.OMGJORT_ALLEREDE}>Vedtaket er allerede omgjort</Radio>
                  <Radio value={GrunnForAvslutning.OMGJOERINGSOPPGAVE_OPPRETTET_VED_FEIL}>
                    Omgjøringsoppgaven er opprettet ved en feil
                  </Radio>
                </>
              }
              errorVedTomInput="Du må velge hvorfor oppgaven skal avsluttes"
            />

            {watch('hvorforAvsluttes') === GrunnForAvslutning.OMGJORT_ALLEREDE && (
              <>
                <Alert variant="warning">
                  Når omgjøringen ikke opprettes ut i fra en omgjøringsoppgave blir ikke den fulle saksbehandlingen i
                  klagesaken fra klage til endelig omgjort vedtak sporbar i saksbehandlingsstatistikken.
                </Alert>
                <Select
                  label="Hvilken behandling omgjorde vedtaket?"
                  {...register('omgjoerendeBehandling', {
                    setValueAs: (value) => (!!value ? value : undefined),
                    required: {
                      value: true,
                      message: 'Du må velge hvilken behandling som omgjorde vedtaket',
                    },
                  })}
                  error={errors.omgjoerendeBehandling?.message}
                >
                  <option value="">Velg</option>
                  {mapResult(behandlingerISakResult, {
                    pending: <Spinner label="Henter behandlinger..." />,
                    success: (data) => (
                      <>
                        {data.map((behandling) => (
                          <option key={behandling.id} value={behandling.id}>
                            {[behandling.behandlingType, behandling.revurderingsaarsak, behandling.virkningstidspunkt]
                              .filter((tekst) => !!tekst)
                              .join(' - ')}
                          </option>
                        ))}
                      </>
                    ),
                  })}
                  <option value="ANNEN_BEHANDLING">En annen behandling (beskriv i begrunnelse)</option>
                </Select>
              </>
            )}

            <Textarea {...register('begrunnelse')} label="Begrunnelse"></Textarea>

            {mapResult(avsluttOmgjoeringsoppgaveResult, {
              error: (error) => (
                <ApiErrorAlert>
                  Kunne ikke avslutte omgjøringsoppgaven på grunn av en feil: {error.detail}
                </ApiErrorAlert>
              ),
              success: (oppgave) => (
                <Alert variant="success">Oppgaven er avsluttet, med merknad: {oppgave.merknad}</Alert>
              ),
            })}

            <HStack gap="4">
              <Button
                variant="secondary"
                size="small"
                onClick={() => setVisSkjema(false)}
                type="button"
                disabled={isPending(avsluttOmgjoeringsoppgaveResult) || isSuccess(avsluttOmgjoeringsoppgaveResult)}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                size="small"
                type="submit"
                loading={isPending(avsluttOmgjoeringsoppgaveResult)}
                disabled={isSuccess(avsluttOmgjoeringsoppgaveResult)}
              >
                Avslutt oppgave
              </Button>
            </HStack>
          </VStack>
        </form>
      ) : (
        <div>
          <Button size="small" variant="secondary" onClick={() => setVisSkjema(true)}>
            Avslutt oppgave
          </Button>
        </div>
      )}
    </>
  )
}
