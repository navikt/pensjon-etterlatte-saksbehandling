import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
  tekstAktivitetspliktVurderingType,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { useForm } from 'react-hook-form'
import { Alert, Box, Button, ErrorMessage, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { JaNei } from '~shared/types/ISvar'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'
import { RedigerAktivitetsgrad } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradWrapperOppgave'

export const RedigerbarAktivtetsgradForm = ({
  aktivitet,
  typeVurdering,
  lagreOgOppdater,
  lagreStatus,
  onAvbryt,
  feilmelding,
}: {
  aktivitet: IAktivitetspliktAktivitetsgrad
  typeVurdering: AktivitetspliktOppgaveVurderingType
  lagreOgOppdater: (formdata: RedigerAktivitetsgrad) => void
  lagreStatus: Result<IAktivitetspliktVurderingNyDto>
  onAvbryt: () => void
  feilmelding: string
}) => {
  const { handleSubmit, register, watch, control } = useForm<RedigerAktivitetsgrad>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: aktivitet,
    },
  })

  const svarAktivitetsgrad = watch('vurderingAvAktivitet.aktivitetsgrad')

  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="space-6">
        <ControlledRadioGruppe
          control={control}
          name="vurderingAvAktivitet.aktivitetsgrad"
          legend="Hva er aktivitetsgraden til bruker?"
          errorVedTomInput="Du må velge en aktivitetsgrad"
          radios={
            <>
              <Radio value={AktivitetspliktVurderingType.AKTIVITET_100}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_100]}
              </Radio>

              <Radio value={AktivitetspliktVurderingType.AKTIVITET_OVER_50}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_OVER_50]}
              </Radio>

              <Radio value={AktivitetspliktVurderingType.AKTIVITET_UNDER_50}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_UNDER_50]}
              </Radio>
            </>
          }
        />
        <HStack gap="space-6">
          <ControlledDatoVelger
            name="vurderingAvAktivitet.fom"
            label="Fra og med"
            control={control}
            description="Fra dato oppgitt"
            errorVedTomInput="Du må velge fra og med dato"
          />
          <ControlledDatoVelger
            name="vurderingAvAktivitet.tom"
            label="Til og med"
            control={control}
            required={false}
            description="Hvis det er oppgitt sluttdato"
          />
        </HStack>

        {watch('typeVurdering') === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER &&
          svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_OVER_50 && (
            <ControlledRadioGruppe
              control={control}
              name="vurderingAvAktivitet.skjoennsmessigVurdering"
              legend="Vil bruker være selvforsørget etter stønaden utløper?"
              errorVedTomInput="Du må vurdere om bruker vil være selvforsørget ved 50-99 % grad aktivitet"
              description="Vurderingen her blir ikke grunnlag for innhold i infobrev, men styrer hvilke oppgaver Gjenny lager for oppfølging av aktiviteten til bruker."
              radios={
                <>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.JA}
                    description="Gjenny lager ingen oppfølgingsoppgave"
                  >
                    {teksterAktivitetspliktSkjoennsmessigVurdering[AktivitetspliktSkjoennsmessigVurdering.JA]}
                  </Radio>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING}
                    description="Bruker skal ha oppfølging. Gjenny oppretter oppfølgingsoppgave. Send oppgave til lokalkontor hvis det ikke er gjort."
                  >
                    {
                      teksterAktivitetspliktSkjoennsmessigVurdering[
                        AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING
                      ]
                    }
                  </Radio>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.NEI}
                    description="Gjenny lager en revurdering neste måned med mulighet for å varsle og fatte sanksjon"
                  >
                    {teksterAktivitetspliktSkjoennsmessigVurdering[AktivitetspliktSkjoennsmessigVurdering.NEI]}
                  </Radio>
                </>
              }
            />
          )}
        {svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50 && watch('harUnntak') !== JaNei.JA && (
          <Box maxWidth="50rem">
            <Alert variant="info">
              Basert på vurderingen du har gjort vil det bli opprettet en revurdering for mulig sanksjon.
            </Alert>
          </Box>
        )}
        <Box maxWidth="60rem">
          <Textarea
            {...register('vurderingAvAktivitet.beskrivelse')}
            label="Beskrivelse"
            description="Beskriv hvordan du har vurdert brukers situasjon"
          />
        </Box>

        {isFailure(lagreStatus) && (
          <ApiErrorAlert>Kunne ikke lagre vurdering: {lagreStatus.error.detail} </ApiErrorAlert>
        )}
        {feilmelding.length > 0 && <ErrorMessage>{feilmelding}</ErrorMessage>}

        <HStack gap="space-4">
          <Button type="submit" loading={isPending(lagreStatus)}>
            Lagre
          </Button>
          {onAvbryt && (
            <Button variant="secondary" onClick={onAvbryt}>
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
