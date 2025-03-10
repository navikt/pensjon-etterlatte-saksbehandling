import { BodyShort, Button, Heading, TextField, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { OpplysningerForEtteroppgjoer } from '~components/etteroppgjoer/oversikt/OpplysningerForEtteroppgjoer'
import { Link } from 'react-router-dom'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { EtteroppgjoerFramTilbakeKnapperad } from '~components/etteroppgjoer/stegmeny/EtteroppgjoerFramTilbakeKnapperad'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import React from 'react'
import { useForm } from 'react-hook-form'
import { NOK } from '~utils/formatering/formatering'

interface Inntekt {
  loenn: number
  afp: number
  naeringsinntekt: number
  utlandsinntekt: number
}

export function EtteroppgjoerOversikt() {
  const etteroppgjoer = useEtteroppgjoer()

  const { watch, register } = useForm<Inntekt>()

  return (
    <VStack gap="8" paddingInline="16" paddingBlock="16 4">
      <Heading level="1" size="large">
        Etteroppgjør {etteroppgjoer.behandling.aar}
      </Heading>
      <BodyShort>Skatteoppgjør mottatt: {formaterDato(etteroppgjoer.behandling.opprettet)}</BodyShort>
      <OpplysningerForEtteroppgjoer opplysninger={etteroppgjoer.opplysninger} />

      <Heading size="medium" level="2">
        Fastsette inntekt
      </Heading>
      <BodyShort>Her skal du fastsette faktisk inntekt for bruker i innvilget periode (april - desember)</BodyShort>
      <VStack width="fit-content" gap="4">
        <TextField {...register('loenn')} label="Lønnsinntekt (eksl. OMS)"></TextField>
        <TextField {...register('afp')} label="AFP"></TextField>
        <TextField {...register('naeringsinntekt')} label="Næringsinntekt"></TextField>
        <TextField {...register('utlandsinntekt')} label="Utlandsinntekt"></TextField>
      </VStack>

      <Heading size="small" level="3">
        Sum:
      </Heading>
      <BodyShort>
        {NOK(+watch('loenn') + +watch('afp') + +watch('naeringsinntekt') + +watch('utlandsinntekt'))}
      </BodyShort>

      <VStack>
        <YtelseEtterAvkorting
          avkortetYtelse={etteroppgjoer.opplysninger.tidligereAvkorting.avkortetYtelse}
          tidligereAvkortetYtelse={[]}
        />
      </VStack>

      <Heading size="small" level="3">
        Resultat
      </Heading>
      <BodyShort>Resultatet av etteroppgjøret er tilbakekreving av {NOK(2000)}</BodyShort>

      <EtteroppgjoerFramTilbakeKnapperad>
        <div>
          <Button
            as={Link}
            to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerSteg.OPPSUMMERING_OG_BREV}`}
          >
            Gå til brev
          </Button>
        </div>
      </EtteroppgjoerFramTilbakeKnapperad>
    </VStack>
  )
}
