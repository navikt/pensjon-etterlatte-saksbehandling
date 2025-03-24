import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { Link } from 'react-router-dom'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/resultatAvForbehandling/ResultatAvForbehandling'
import { UtfallAvForbehandling } from '~shared/types/Etteroppgjoer'

export const OversiktOverEtteroppgjoer = () => {
  const etteroppgjoer = useEtteroppgjoer()

  const utfallAvForbehandling = UtfallAvForbehandling.SEND_VARSELBREV

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {etteroppgjoer.behandling.aar}
      </Heading>
      <BodyShort>
        <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
      </BodyShort>
      <Inntektsopplysninger />
      <FastsettFaktiskInntekt forbehandlingId={etteroppgjoer.behandling.id} />
      <ResultatAvForbehandling utfallAvForbehandling={utfallAvForbehandling} />

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
        <HStack width="100%" justify="center">
          <div>
            {/* @ts-expect-error ignorerer denne til ekte data flyt er på plass, dette er kun for test */}
            {utfallAvForbehandling !== UtfallAvForbehandling.FERDIGSTILL_UTEN_ENDRING ? (
              <Button
                as={Link}
                to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerSteg.OPPSUMMERING_OG_BREV}`}
              >
                Gå til brev
              </Button>
            ) : (
              <Button>Ferdigstill</Button>
            )}
          </div>
        </HStack>
      </Box>
    </VStack>
  )
}
