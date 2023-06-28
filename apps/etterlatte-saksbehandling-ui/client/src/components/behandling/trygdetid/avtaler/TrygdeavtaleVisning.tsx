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
    </TrygdeavtaleBody>
  )
}

const TrygdeavtaleBody = styled.div`
  margin-bottom: 2em;
`
