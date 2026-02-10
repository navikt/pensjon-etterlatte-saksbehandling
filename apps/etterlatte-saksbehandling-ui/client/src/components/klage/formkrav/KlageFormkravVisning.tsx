import { useKlage } from '~components/klage/useKlage'
import { BodyShort, Box, Button, Heading, HStack } from '@navikt/ds-react'
import { formaterVedtakType } from '~utils/formatering/formatering'
import { formaterDatoMedKlokkeslett, formaterKanskjeStringDato } from '~utils/formatering/dato'
import React from 'react'
import { JaNei } from '~shared/types/ISvar'
import { useNavigate } from 'react-router-dom'
import { nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

export function KlageFormkravVisning() {
  const klage = useKlage()
  const navigate = useNavigate()

  if (!klage) return

  const formkrav = klage.formkrav?.formkrav
  const vedtak = formkrav?.vedtaketKlagenGjelder
  const saksbehandler = klage.formkrav?.saksbehandler

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Formkrav og klagefrist
        </Heading>
      </Box>
      <Box paddingBlock="space-8" paddingInline="space-16 space-8">
        <Heading size="small" spacing>
          Hvilket vedtak klages det på?
        </Heading>
        <BodyShort spacing>
          {vedtak &&
            `Vedtak ${vedtak.id} om ${formaterVedtakType(vedtak.vedtakType!!)} - ` +
              formaterKanskjeStringDato(vedtak.datoAttestert)}
        </BodyShort>

        <Heading size="small" spacing>
          Er klager part i saken?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagerPartISaken == JaNei.JA ? 'Ja' : 'Nei'} </BodyShort>

        <Heading size="small" spacing>
          Er klagen signert?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagenSignert == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Klages det på konkrete elementer i vedtaket?
        </Heading>
        <BodyShort spacing>{formkrav?.gjelderKlagenNoeKonkretIVedtaket == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Er klagen framsatt innenfor klagefristen?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagenFramsattInnenFrist == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Totalvurdering
        </Heading>
        <TekstMedMellomrom spacing>{formkrav?.begrunnelse}</TekstMedMellomrom>

        <Heading size="small" spacing>
          Saksbehandler
        </Heading>
        <BodyShort spacing>
          {saksbehandler?.ident} -{' '}
          {saksbehandler?.tidspunkt
            ? formaterDatoMedKlokkeslett(saksbehandler.tidspunkt)
            : 'Mangler tidspunkt for vurdering'}
        </BodyShort>
      </Box>
      <HStack justify="center">
        <Button onClick={() => navigate(nesteSteg(klage, 'formkrav'))}>Neste side</Button>
      </HStack>
    </>
  )
}
