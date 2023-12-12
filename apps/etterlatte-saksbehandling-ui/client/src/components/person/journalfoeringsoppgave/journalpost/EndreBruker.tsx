import { Bruker, BrukerIdType } from '~shared/types/Journalpost'
import React from 'react'
import { Alert, BodyShort, Heading } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'

const formaterType = (type: BrukerIdType) => {
  switch (type) {
    case BrukerIdType.ORGNR:
      return 'Orgnr.'
    case BrukerIdType.AKTOERID:
      return 'Aktørid'
    case BrukerIdType.FNR:
      return 'Fødselsnummer'
  }
}

export const EndreBruker = ({ bruker }: { bruker: Bruker }) => {
  // TODO:
  //  - Hente navn fra PDL (ikke grunnlag siden det ikke nødvendigvis finnes)
  //  - Gjøre det mulig å endre bruker på journalposten
  //  - Konvertere aktørID til fnr i backend (dokarkiv endrer fra FNR -> AKTOERID når journalposten oppdateres)

  return (
    <div>
      <Heading size="small" spacing>
        Bruker
      </Heading>

      <BodyShort as="div" spacing>
        {!!bruker.type && bruker.type !== BrukerIdType.FNR && (
          <Alert variant="info" inline>
            {formaterType(bruker.type)}
          </Alert>
        )}
        <KopierbarVerdi value={bruker.id!!} />
      </BodyShort>
    </div>
  )
}
