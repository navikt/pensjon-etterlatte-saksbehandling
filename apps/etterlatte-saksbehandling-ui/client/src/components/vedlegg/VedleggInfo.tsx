import React from 'react'
import { Alert } from '@navikt/ds-react'

const VedleggInfo = (props: { vedleggTittel: string }) => {
  const { vedleggTittel } = props

  const vedleggInfo = [
    {
      tittel: 'Utfall ved beregning av omstillingsstønad',
      infotekst:
        'Skriv inn hvilken inntekt som er lagt til grunn. Her kan du også legge inn om institusjonsopphold e.l. dersom det skulle være aktuelt.',
    },
  ]

  const skalViseInfoboks = vedleggInfo.find((info) => info.tittel === vedleggTittel)

  return skalViseInfoboks ? <Alert variant="info">{skalViseInfoboks.infotekst}</Alert> : null
}

export default VedleggInfo
