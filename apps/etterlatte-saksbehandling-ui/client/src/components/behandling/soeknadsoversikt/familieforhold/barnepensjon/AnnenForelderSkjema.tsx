import { ExternalLinkIcon, FloppydiskIcon, PencilIcon } from '@navikt/aksel-icons'
import { BodyShort, Box, Button, Heading, Link, Radio, Textarea, TextField } from '@navikt/ds-react'
import {
  AnnenForelder,
  AnnenForelderVurdering,
  Personopplysninger,
  teksterAnnenForelderVurdering,
} from '~shared/types/grunnlag'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isPending } from '~shared/api/apiUtils'
import { redigerAnnenForelder } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { formaterDato } from '~utils/formatering/dato'

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
    watch,
    handleSubmit,
    formState: { errors },
    control,
  } = useForm<AnnenForelder>({
    defaultValues: { ...personopplysninger?.annenForelder },
  })

  return (
    <Box paddingBlock="5 0" maxWidth="25rem">
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
              {personopplysninger.annenForelder?.vurdering && (
                <Box paddingBlock="2 4">
                  <Heading size="xsmall" level="4">
                    Vurdering
                  </Heading>
                  <BodyShort>{teksterAnnenForelderVurdering[personopplysninger.annenForelder?.vurdering]}</BodyShort>
                </Box>
              )}
              {personopplysninger.annenForelder?.begrunnelse && (
                <Box paddingBlock="0 4">
                  <Heading size="xsmall" level="4">
                    Begrunnelse
                  </Heading>
                  <BodyShort>{personopplysninger.annenForelder?.begrunnelse}</BodyShort>
                </Box>
              )}
              {personopplysninger.annenForelder.navn && (
                <Box paddingBlock="0 4">
                  <Heading size="xsmall" level="4">
                    Navn
                  </Heading>
                  <BodyShort>{personopplysninger.annenForelder?.navn}</BodyShort>
                </Box>
              )}
              {personopplysninger.annenForelder.foedselsdato && (
                <Box paddingBlock="0 4">
                  <Heading size="xsmall" level="4">
                    Fødselsdato
                  </Heading>
                  <BodyShort>{formaterDato(personopplysninger.annenForelder?.foedselsdato)}</BodyShort>
                </Box>
              )}
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
                <Radio
                  key={verdi}
                  value={verdi}
                  description={
                    verdi === AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER
                      ? 'Huk av hvis det ikke er registrert en annen juridisk forelder enn avdød. Dette gir sats som foreldreløs.'
                      : ''
                  }
                >
                  {tekst}
                </Radio>
              )
            })}
          />
          <Heading size="xsmall" level="4" spacing>
            {watch().vurdering && teksterAnnenForelderVurdering[watch().vurdering]}
          </Heading>

          {redigerModus && <>{watch}</>}
          {watch().vurdering === AnnenForelderVurdering.FORELDER_UTEN_IDENT_I_PDL && (
            <>
              <TextField {...register('navn')} label="Navn" />
              <ControlledDatoVelger name="foedselsdato" label="Fødselsdato" control={control} />
            </>
          )}
          {watch().vurdering !== AnnenForelderVurdering.AVDOED_FORELDER_UTEN_IDENT_I_PDL && (
            <Textarea
              {...register('begrunnelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Begrunnelse"
              error={errors.begrunnelse?.message}
            />
          )}
          {watch().vurdering === AnnenForelderVurdering.AVDOED_FORELDER_UTEN_IDENT_I_PDL && (
            <>
              <Link href="https://nav.no" target="_blank" rel="noopener noreferrer">
                Følg rutine for å registrere avdød forelder
                <ExternalLinkIcon title="" />
              </Link>
              <BodyShort>Når rutinen er fullført, må du oppdatere grunnlaget før saken kan behandles videre.</BodyShort>
            </>
          )}
          <Box paddingBlock="1 0">
            <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(redigerStatus)}>
              Lagre
            </Button>
          </Box>
        </form>
      )}
    </Box>
  )
}
