import { Button, HStack, Radio, ReadMore, Select, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForm } from 'react-hook-form'
import {
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  IAktivitetspliktVurdering,
  IValgJaNei,
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

interface AktivitetspliktVurderingValues {
  aktivitetsplikt: IValgJaNei | null
  aktivitetsgrad: AktivitetspliktVurderingType | ''
  unntak: IValgJaNei | null
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

  const [opprettetAktivitetsgrad, opprettAktivitetsgrad] = useApiCall(opprettAktivitspliktAktivitetsgradForBehandling)
  const [opprettetUnntak, opprettUnntak] = useApiCall(opprettAktivitspliktUnntakForBehandling)
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForBehandling)

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
  } = useForm<AktivitetspliktVurderingValues>({
    defaultValues: AktivitetspliktVurderingValuesDefault,
  })

  const opprettVurdering = (data: AktivitetspliktVurderingValues) => {
    if (data.aktivitetsplikt === IValgJaNei.NEI || data.unntak === IValgJaNei.JA) {
      opprettUnntak(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: undefined,
            unntak:
              data.aktivitetsplikt === IValgJaNei.NEI
                ? AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                : (data.midlertidigUnntak as AktivitetspliktUnntakType),
            beskrivelse: data.beskrivelse,
            tom:
              data.sluttdato && data.aktivitetsplikt === IValgJaNei.JA
                ? new Date(data.sluttdato).toISOString()
                : undefined,
          },
        },
        () => {
          resetManglerAktivitetspliktVurdering()
        }
      )
    } else {
      opprettAktivitetsgrad(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: undefined,
            aktivitetsgrad: data.aktivitetsgrad as AktivitetspliktVurderingType,
            beskrivelse: data.beskrivelse,
            fom: new Date().toISOString(),
          },
        },
        () => {
          resetManglerAktivitetspliktVurdering()
        }
      )
    }
  }

  useEffect(() => {
    hent({ sakId: behandling.sakId, behandlingId: behandling.id }, (result) => {
      setVurdering(result)
      if (result) resetManglerAktivitetspliktVurdering()
    })
  }, [])

  const harAktivitetsplikt = watch('aktivitetsplikt')
  const harUnntak = watch('unntak')

  return (
    <AktivitetspliktVurderingWrapper>
      {(isSuccess(opprettetUnntak) || isSuccess(opprettetAktivitetsgrad)) && (
        <Toast melding="Vurdering av aktivitetsplikt er lagret" />
      )}
      <div>
        <HStack gap="12">
          <Spinner label="Henter vurdering av aktivitetsplikt" visible={isPending(hentet)} />

          <VStack gap="4">
            <ControlledRadioGruppe
              name="aktivitetsplikt"
              control={control}
              errorVedTomInput="Du må velge om bruker har aktivitetsplikt"
              legend="Har bruker aktivitetsplikt?"
              radios={
                <>
                  <Radio size="small" value={IValgJaNei.JA}>
                    Ja
                  </Radio>
                  <Radio size="small" value={IValgJaNei.NEI}>
                    {
                      tekstAktivitetspliktUnntakType[
                        AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                      ]
                    }
                  </Radio>
                </>
              }
            />

            <ReadMore header="Dette mener vi med lav inntekt">
              Med lav inntekt menes det at den gjenlevende ikke har hatt en gjennomsnittlig årlig arbeidsinntekt som
              overstiger to ganger grunnbeløpet for hvert av de fem siste årene. I tillegg må den årlige inntekten ikke
              ha oversteget tre ganger grunnbeløpet hvert av de siste to årene før dødsfallet.
            </ReadMore>
            {harAktivitetsplikt === IValgJaNei.JA && (
              <>
                <ControlledRadioGruppe
                  name="unntak"
                  control={control}
                  errorVedTomInput="Du må velge om bruker har unntak fra aktivitetsplikt"
                  legend="Er det unntak for bruker?"
                  radios={
                    <>
                      <Radio size="small" value={IValgJaNei.JA}>
                        Ja
                      </Radio>
                      <Radio size="small" value={IValgJaNei.NEI}>
                        Nei
                      </Radio>
                    </>
                  }
                />
                {harUnntak === IValgJaNei.JA && (
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
                    <ControlledDatoVelger
                      name="sluttdato"
                      label="Angi sluttdato for unntaksperiode"
                      description="Du trenger ikke legge til en sluttdato hvis den ikke er tilgjengelig"
                      control={control}
                      required={false}
                    />
                  </>
                )}
                {harUnntak === IValgJaNei.NEI && (
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
            <Button
              loading={isPending(opprettetAktivitetsgrad) || isPending(opprettetUnntak)}
              variant="primary"
              type="button"
              size="small"
              onClick={handleSubmit(opprettVurdering)}
            >
              Lagre vurdering
            </Button>
          </VStack>

          {vurdering && <>{vurdering.aktivitet?.aktivitetsgrad}</>}
        </HStack>
        {mapFailure(opprettetUnntak, (error) => (
          <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av unntak'}</ApiErrorAlert>
        ))}
        {mapFailure(opprettetAktivitetsgrad, (error) => (
          <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppretting av aktivitetsgrad'}</ApiErrorAlert>
        ))}
      </div>
    </AktivitetspliktVurderingWrapper>
  )
}

const AktivitetspliktVurderingWrapper = styled.div`
  max-width: 500px;
`
