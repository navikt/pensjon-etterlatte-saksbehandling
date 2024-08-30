import { FloppydiskIcon, PencilIcon, PersonIcon } from '@navikt/aksel-icons'
import { BodyShort, Button, Heading, Radio, Textarea } from '@navikt/ds-react'
import styled from 'styled-components'
import { IconSize } from '~shared/types/Icon'
import { AnnenForelder, Personopplysninger, teksterAnnenForelderVurdering } from '~shared/types/grunnlag'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isPending } from '~shared/api/apiUtils'
import { redigerAnnenForelder } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'

const AnnenForelderBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
  display: flex;
`

const IconWrapper = styled.span`
  width: 2.5rem;
`

const PersonInfoWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`

type Props = {
  behandlingId: string
  personopplysninger: Personopplysninger
}

export const AnnenForelderSkjema = ({ behandlingId, personopplysninger }: Props) => {
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [redigerStatus, redigerAnnenForelderRequest] = useApiCall(redigerAnnenForelder)

  const lagreAnnenForelder = (annenForelder: AnnenForelder) => {
    redigerAnnenForelderRequest(
      {
        behandlingId: behandlingId,
        annenForelder: annenForelder,
      },
      () => {
        setTimeout(() => window.location.reload(), 2000)
      }
    )
  }

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
  } = useForm<AnnenForelder>({
    defaultValues: { ...personopplysninger?.annenForelder },
  })

  return (
    <AnnenForelderBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size="small" level="3">
          Annen forelder
        </Heading>
        {!redigerModus && (
          <>
            {personopplysninger.annenForelder == null ? (
              <Button variant="secondary" onClick={() => setRedigerModus(true)}>
                Legg til vurdering
              </Button>
            ) : (
              <>
                <BodyShort>{formaterEnumTilLesbarString(personopplysninger.annenForelder?.vurdering)}</BodyShort>
                <BodyShort>{personopplysninger.annenForelder?.begrunnelse}</BodyShort>
              </>
            )}
            {personopplysninger.annenForelder && (
              <Button
                type="button"
                variant="secondary"
                size="small"
                icon={<PencilIcon aria-hidden />}
                disabled={redigerModus}
                onClick={() => setRedigerModus(true)}
              >
                Rediger
              </Button>
            )}
          </>
        )}
        {redigerModus && (
          <form onSubmit={handleSubmit(lagreAnnenForelder)}>
            <ControlledRadioGruppe
              name="vurdering"
              control={control}
              errorVedTomInput="Du må velge et svar"
              legend=""
              hideLegend={true}
              radios={Object.entries(teksterAnnenForelderVurdering).map(([verdi, tekst]) => {
                return (
                  <Radio key={verdi} value={verdi}>
                    {tekst}
                  </Radio>
                )
              })}
            />
            <Textarea
              {...register('begrunnelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Beskrivelse"
              error={errors.begrunnelse?.message}
            />
            <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(redigerStatus)}>
              Lagre
            </Button>
          </form>
        )}
      </PersonInfoWrapper>
    </AnnenForelderBorder>
  )
}
