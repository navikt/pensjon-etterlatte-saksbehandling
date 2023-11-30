import { BodyShort } from '@navikt/ds-react'
import styled from 'styled-components'
import { Trygdeavtale, TrygdetidAvtale, TrygdetidAvtaleKriteria } from '~shared/api/trygdetid'

interface TrygdeavtaleVisningProps {
  trygdeavtale: Trygdeavtale
  avtaler: TrygdetidAvtale[]
  kriterier: TrygdetidAvtaleKriteria[]
}

export const TrygdeavtaleVisning = ({ trygdeavtale, avtaler, kriterier }: TrygdeavtaleVisningProps) => {
  const avtale = avtaler.find((avtale) => trygdeavtale.avtaleKode === avtale.kode)
  const avtaleDato = avtale?.datoer.find((dato) => trygdeavtale.avtaleDatoKode === dato.kode)
  const avtaleKriteria = kriterier.find((kriteria) => trygdeavtale.avtaleKriteriaKode === kriteria.kode)

  return (
    <TrygdeavtaleBody>
      <BodyShort>
        {avtale && avtale.beskrivelse}
        {avtaleDato && <> - {avtaleDato.beskrivelse}</>}
      </BodyShort>
      {avtaleKriteria && <BodyShort>{avtaleKriteria.beskrivelse}</BodyShort>}
      {trygdeavtale.personKrets && (
        <BodyShort>Er avdøde i personkretsen i denne avtalen? {trygdeavtale.personKrets}</BodyShort>
      )}
      {trygdeavtale.arbInntekt1G && (
        <BodyShort>
          Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet? {trygdeavtale.arbInntekt1G}
        </BodyShort>
      )}
      {trygdeavtale.arbInntekt1GKommentar && <BodyShort>Kommentar: {trygdeavtale.arbInntekt1GKommentar}</BodyShort>}
      {trygdeavtale.beregArt50 && (
        <BodyShort>Beregning etter artikkel 50 (EØS-forordning 883/2004)? {trygdeavtale.beregArt50}</BodyShort>
      )}
      {trygdeavtale.beregArt50Kommentar && <BodyShort>Kommentar: {trygdeavtale.beregArt50Kommentar}</BodyShort>}
      {trygdeavtale.nordiskTrygdeAvtale && (
        <BodyShort>
          Nordisk trygdeavtale: Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?{' '}
          {trygdeavtale.nordiskTrygdeAvtale}
        </BodyShort>
      )}
      {trygdeavtale.nordiskTrygdeAvtaleKommentar && (
        <BodyShort>Kommentar: {trygdeavtale.nordiskTrygdeAvtaleKommentar}</BodyShort>
      )}
    </TrygdeavtaleBody>
  )
}

const TrygdeavtaleBody = styled.div`
  margin-bottom: 2em;
`
