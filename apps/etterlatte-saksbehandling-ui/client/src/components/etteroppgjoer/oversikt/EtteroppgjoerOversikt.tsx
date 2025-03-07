import { BodyShort, Button, Heading, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { OpplysningerForEtteroppgjoer } from '~components/etteroppgjoer/oversikt/OpplysningerForEtteroppgjoer'
import { Link } from 'react-router-dom'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { EtteroppgjoerFramTilbakeKnapperad } from '~components/etteroppgjoer/stegmeny/EtteroppgjoerFramTilbakeKnapperad'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'

export function EtteroppgjoerOversikt() {
  const etteroppgjoer = useEtteroppgjoer()

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
