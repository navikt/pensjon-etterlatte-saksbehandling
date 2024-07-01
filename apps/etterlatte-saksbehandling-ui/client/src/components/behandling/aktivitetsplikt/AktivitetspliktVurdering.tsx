import { Alert, Button, Heading, HStack, Radio, ReadMore, Select, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isFailure, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForm } from 'react-hook-form'
import {
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import {
  hentAktivitspliktVurderingForBehandling,
  opprettAktivitspliktAktivitetsgradForBehandling,
  opprettAktivitspliktUnntakForBehandling,
} from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import { Toast } from '~shared/alerts/Toast'
import { AktivitetspliktVurderingVisning } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurderingVisning'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { JaNei } from '~shared/types/ISvar'

interface AktivitetspliktVurderingValues {
  aktivitetsplikt: JaNei | null
  aktivitetsgrad: AktivitetspliktVurderingType | ''
  unntak: JaNei | null
  midlertidigUnntak: AktivitetspliktUnntakType | ''
  sluttdato?: Date | null
  beskrivelse: string
}

const AktivitetspliktVurderingValuesDefault: AktivitetspliktVurderingValues = {
  aktivitetsplikt: null,
  aktivitetsgrad: '',
  unntak: null,
  midlertidigUnntak: '',
  sluttdato: undefined,
  beskrivelse: '',
}

export const AktivitetspliktVurdering = ({
  behandling,
  resetManglerAktivitetspliktVurdering,
}: {
  behandling: IDetaljertBehandling
  resetManglerAktivitetspliktVurdering: () => void
}) => {
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurdering>()
  const [visForm, setVisForm] = useState(false)

  const [opprettetAktivitetsgrad, opprettAktivitetsgrad] = useApiCall(opprettAktivitspliktAktivitetsgradForBehandling)
  const [opprettetUnntak, opprettUnntak] = useApiCall(opprettAktivitspliktUnntakForBehandling)
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
    reset,
  } = useForm<AktivitetspliktVurderingValues>({
    defaultValues: AktivitetspliktVurderingValuesDefault,
  })

  const opprettVurdering = (data: AktivitetspliktVurderingValues) => {
    if (data.aktivitetsplikt === JaNei.NEI || data.unntak === JaNei.JA) {
      opprettUnntak(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: vurdering?.unntak ? vurdering.unntak.id : undefined,
            unntak:
              data.aktivitetsplikt === JaNei.NEI
                ? AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                : (data.midlertidigUnntak as AktivitetspliktUnntakType),
            beskrivelse: data.beskrivelse,
            tom:
              data.sluttdato && data.aktivitetsplikt === JaNei.JA ? new Date(data.sluttdato).toISOString() : undefined,
          },
        },
        () => {
          resetManglerAktivitetspliktVurdering()
          hent({ sakId: behandling.sakId, behandlingId: behandling.id }, (result) => {
            setVurdering(result)
            setVisForm(false)
          })
        }
      )
    } else {
      opprettAktivitetsgrad(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: vurdering?.aktivitet ? vurdering.aktivitet.id : undefined,
            aktivitetsgrad: data.aktivitetsgrad as AktivitetspliktVurderingType,
            beskrivelse: data.beskrivelse,
            fom: new Date().toISOString(),
          },
        },
        () => {
          resetManglerAktivitetspliktVurdering()
          hent({ sakId: behandling.sakId, behandlingId: behandling.id }, (result) => {
            setVurdering(result)
            setVisForm(false)
          })
        }
      )
    }
  }

  useEffect(() => {
    if (!vurdering) {
      hent(
        { sakId: behandling.sakId, behandlingId: behandling.id },
        (result) => {
          setVurdering(result)
          if (result) resetManglerAktivitetspliktVurdering()
        },
        (error) => {
          if (error.status === 404) setVisForm(true)
        }
      )
    }
  }, [])

  useEffect(() => {
    if (visForm && vurdering) {
      if (vurdering.aktivitet) {
        reset({
          aktivitetsplikt: JaNei.JA,
          aktivitetsgrad: vurdering.aktivitet.aktivitetsgrad,
          unntak: JaNei.NEI,
          midlertidigUnntak: '',
          sluttdato: undefined,
          beskrivelse: vurdering.aktivitet.beskrivelse,
        })
      }
      if (vurdering.unntak) {
        if (vurdering.unntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT) {
          reset({
            aktivitetsplikt: JaNei.NEI,
            aktivitetsgrad: '',
            unntak: null,
            midlertidigUnntak: '',
            sluttdato: undefined,
            beskrivelse: vurdering.unntak.beskrivelse,
          })
        } else {
          reset({
            aktivitetsplikt: JaNei.JA,
            aktivitetsgrad: '',
            unntak: JaNei.JA,
            midlertidigUnntak: vurdering.unntak.unntak,
            sluttdato: vurdering.unntak.tom ? new Date(vurdering.unntak.tom) : undefined,
            beskrivelse: vurdering.unntak.beskrivelse,
          })
        }
      }
    }
  }, [visForm])

  const harAktivitetsplikt = watch('aktivitetsplikt')
  const harUnntak = watch('unntak')

  return (
    <AktivitetspliktVurderingWrapper>
      <Heading size="small" spacing>
        Vurdering av aktivitetsplikt
      </Heading>
      {(isSuccess(opprettetUnntak) || isSuccess(opprettetAktivitetsgrad)) && (
        <Toast melding="Vurdering av aktivitetsplikt er lagret" />
      )}
      <Spinner label="Henter vurdering av aktivitetsplikt" visible={isPending(hentet)} />
      {visForm && redigerbar && (
        <VStack gap="4">
          <ControlledRadioGruppe
            name="aktivitetsplikt"
            control={control}
            errorVedTomInput="Du må velge om bruker har aktivitetsplikt"
            legend="Har bruker aktivitetsplikt?"
            radios={
              <>
                <Radio size="small" value={JaNei.JA}>
                  Ja
                </Radio>
                <Radio size="small" value={JaNei.NEI}>
                  {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT]}
                </Radio>
              </>
            }
          />

          <ReadMore header="Dette mener vi med lav inntekt">
            Med lav inntekt menes det at den gjenlevende ikke har hatt en gjennomsnittlig årlig arbeidsinntekt som
            overstiger to ganger grunnbeløpet for hvert av de fem siste årene. I tillegg må den årlige inntekten ikke ha
            oversteget tre ganger grunnbeløpet hvert av de siste to årene før dødsfallet.
          </ReadMore>
          {harAktivitetsplikt === JaNei.JA && (
            <>
              <ControlledRadioGruppe
                name="unntak"
                control={control}
                errorVedTomInput="Du må velge om bruker har unntak fra aktivitetsplikt"
                legend="Er det unntak for bruker?"
                radios={
                  <>
                    <Radio size="small" value={JaNei.JA}>
                      Ja
                    </Radio>
                    <Radio size="small" value={JaNei.NEI}>
                      Nei
                    </Radio>
                  </>
                }
              />
              {harUnntak === JaNei.JA && (
                <>
                  <Select
                    label="Hvilket midlertidig unntak er det?"
                    {...register('midlertidigUnntak', {
                      required: { value: true, message: 'Du må velge midlertidig unntak' },
                    })}
                    error={errors.midlertidigUnntak?.message}
                  >
                    <option value="">Velg hvilke unntak</option>
                    {Object.values(AktivitetspliktUnntakType)
                      .filter(
                        (unntak) => unntak !== AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                      )
                      .map((type) => (
                        <option key={type} value={type}>
                          {tekstAktivitetspliktUnntakType[type]}
                        </option>
                      ))}
                  </Select>
                  <Alert variant="warning">
                    Lag oppfølgingsoppgave i Gosys med frist utfra angitt sluttdato for unntaksperiode, eller sett en
                    passende frist.
                  </Alert>

                  <ControlledDatoVelger
                    name="sluttdato"
                    label="Angi sluttdato for unntaksperiode"
                    description="Du trenger ikke legge til en sluttdato hvis den ikke er tilgjengelig"
                    control={control}
                    required={false}
                  />
                </>
              )}
              {harUnntak === JaNei.NEI && (
                <Select
                  label="Hva er brukers aktivitetsgrad?"
                  {...register('aktivitetsgrad', {
                    required: { value: true, message: 'Du må velge aktivitetsgrad' },
                  })}
                  error={errors.aktivitetsgrad?.message}
                >
                  <option value="">Velg hvilken grad</option>
                  {Object.values(AktivitetspliktVurderingType).map((type) => (
                    <option key={type} value={type}>
                      {tekstAktivitetspliktVurderingType[type]}
                    </option>
                  ))}
                </Select>
              )}
            </>
          )}
          <Textarea
            label="Vurdering"
            {...register('beskrivelse', {
              required: { value: true, message: 'Du må fylle inn vurdering' },
            })}
            error={errors.beskrivelse?.message}
          />
          <HStack gap="4">
            {vurdering && (
              <Button
                loading={isPending(opprettetAktivitetsgrad) || isPending(opprettetUnntak)}
                variant="secondary"
                type="button"
                size="small"
                onClick={() => setVisForm(false)}
              >
                Avbryt
              </Button>
            )}
            <Button
              loading={isPending(opprettetAktivitetsgrad) || isPending(opprettetUnntak)}
              variant="primary"
              type="button"
              size="small"
              onClick={handleSubmit(opprettVurdering)}
            >
              Lagre vurdering
            </Button>
          </HStack>
        </VStack>
      )}
      {!isPending(hentet) && !visForm && (
        <AktivitetspliktVurderingVisning
          vurdering={vurdering}
          visForm={() => setVisForm(true)}
          erRedigerbar={redigerbar}
        />
      )}
      {mapFailure(opprettetUnntak, (error) => (
        <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av unntak'}</ApiErrorAlert>
      ))}
      {mapFailure(opprettetAktivitetsgrad, (error) => (
        <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av aktivitetsgrad'}</ApiErrorAlert>
      ))}
      {isFailure(hentet) && hentet.error.status !== 404 && (
        <ApiErrorAlert>{hentet.error.detail || 'Det oppsto en feil ved henting av vurdering'}</ApiErrorAlert>
      )}
    </AktivitetspliktVurderingWrapper>
  )
}

const AktivitetspliktVurderingWrapper = styled.div`
  max-width: 500px;
`
