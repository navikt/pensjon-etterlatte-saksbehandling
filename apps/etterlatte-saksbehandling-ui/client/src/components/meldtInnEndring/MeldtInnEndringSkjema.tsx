import { OppgaveDTO } from '~shared/types/oppgave'
import { MeldtInnEndringHandlingValgt } from '~components/meldtInnEndring/MeldtInnEndring'
import { useForm } from 'react-hook-form'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { BodyShort, Button, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { useEffect } from 'react'
import { SakType } from '~shared/types/sak'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { navigerTilPersonOversikt } from '~components/person/lenker/navigerTilPersonOversikt'
import { PersonOversiktFane } from '~components/person/Person'

interface Skjema {
  revurderingsaarsak: Revurderingaarsak
  begrunnelse: string
}

interface Props {
  oppgave: OppgaveDTO
  meldtInnEndringHandlingValgt: MeldtInnEndringHandlingValgt
  setMeldtInnEndringHandlingValgt: (meldtInnEndringHandlingValgt: MeldtInnEndringHandlingValgt) => void
}

export const MeldtInnEndringSkjema = ({
  oppgave,
  meldtInnEndringHandlingValgt,
  setMeldtInnEndringHandlingValgt,
}: Props) => {
  const [ferdigstillOppgaveResult, ferdigstillOppgaveRequest] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [opprettRevurderingResult, opprettRevurderingRequest] = useApiCall(opprettRevurderingApi)

  const [hentStoettedeRevurderingsAarsakerResult, hentStoettedeRevurderingsAarsakerFetch] =
    useApiCall(hentStoettedeRevurderinger)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Skjema>()

  const sorterRevurderingsaarsakerAlfabetisk = (revurderingsaarsaker: Revurderingaarsak[]): Revurderingaarsak[] => {
    return revurderingsaarsaker.toSorted((first, last) => {
      if (tekstRevurderingsaarsak[first].trim().toLowerCase() > tekstRevurderingsaarsak[last].trim().toLowerCase()) {
        return 1
      }
      return -1
    })
  }

  const avsluttOppgaveEllerOpprettRevurdering = (data: Skjema) => {
    if (meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.AVSLUTT_OPPGAVE) {
      ferdigstillOppgaveRequest({ id: oppgave.id, merknad: data.begrunnelse }, () => {
        navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)
      })
    } else if (meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.OPPRETT_REVURDERING) {
      opprettRevurderingRequest({ sakId: oppgave.sakId, aarsak: data.revurderingsaarsak }, () => {
        ferdigstillOppgaveRequest({ id: oppgave.id, merknad: data.begrunnelse }, () => {
          navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)
        })
      })
    }
  }

  useEffect(() => {
    if (meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.OPPRETT_REVURDERING) {
      hentStoettedeRevurderingsAarsakerFetch({ sakType: SakType.OMSTILLINGSSTOENAD })
    }
  }, [meldtInnEndringHandlingValgt])

  return (
    <form onSubmit={handleSubmit(avsluttOppgaveEllerOpprettRevurdering)}>
      <VStack gap="space-4">
        {meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.OPPRETT_REVURDERING &&
          mapResult(hentStoettedeRevurderingsAarsakerResult, {
            pending: <Spinner label="Henter revurderingsårsaker..." />,
            error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
            success: (revurderingsaarsaker) => (
              <Select
                {...register('revurderingsaarsak', {
                  required: { value: true, message: 'Du må velge en årsak for revurdering' },
                })}
                label="Årsak for revurdering"
                error={errors.revurderingsaarsak?.message}
              >
                <option value="">Velg årsak</option>
                {sorterRevurderingsaarsakerAlfabetisk(revurderingsaarsaker).map((aarsak, index) => (
                  <option key={index} value={aarsak}>
                    {tekstRevurderingsaarsak[aarsak]}
                  </option>
                ))}
              </Select>
            ),
          })}
        {meldtInnEndringHandlingValgt !== MeldtInnEndringHandlingValgt.INGEN && (
          <>
            {meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.AVSLUTT_OPPGAVE && (
              <BodyShort>
                Når du avslutter oppgaven, kan du ikke gjøre endringer. Du finner historikken i saksoversikten under
                ferdigstilte oppgaver. Husk å gi beskjed til bruker før du avslutter.
              </BodyShort>
            )}
            <Textarea {...register('begrunnelse')} label="Begrunnelse (valgfritt)" />
          </>
        )}

        {isFailureHandler({
          apiResult: ferdigstillOppgaveResult,
          errorMessage: 'Feil under ferdigstilling av oppgave',
        })}

        {isFailureHandler({
          apiResult: opprettRevurderingResult,
          errorMessage: 'Feil under opprettelse av revurdering',
        })}

        <HStack justify="space-between">
          <Button
            variant="secondary"
            type="button"
            disabled={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}
            onClick={() => setMeldtInnEndringHandlingValgt(MeldtInnEndringHandlingValgt.INGEN)}
          >
            Avbryt
          </Button>
          {meldtInnEndringHandlingValgt !== MeldtInnEndringHandlingValgt.INGEN && (
            <Button loading={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}>
              {meldtInnEndringHandlingValgt === MeldtInnEndringHandlingValgt.AVSLUTT_OPPGAVE ? 'Avslutt' : 'Opprett'}
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
