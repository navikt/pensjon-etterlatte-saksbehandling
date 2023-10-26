import { Select } from '@navikt/ds-react'
import { ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import DokumenterForLand from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumenterForLand'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'

const Flex = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;

  div:first-child {
    margin-bottom: auto;
  }
`

export default function LandRad({
  lesevisning = false,
  landMedDokumenter,
  oppdaterLandMedDokumenter,
  landListe,
}: {
  lesevisning: boolean
  landMedDokumenter: LandMedDokumenter
  oppdaterLandMedDokumenter: (oppdatertLandMedDokumenter: LandMedDokumenter) => void
  landListe: ILand[]
}) {
  return (
    <Flex>
      <Select
        readOnly={lesevisning}
        style={{ marginRight: '2rem', minWidth: '10rem', maxWidth: '12rem' }}
        label={lesevisning ? 'Valgt lang' : 'Velg land'}
        value={landMedDokumenter.landIsoKode || ''}
        onChange={(e) => {
          const landMedDokumenterMedLandKode = { ...landMedDokumenter, landIsoKode: e.target.value }
          oppdaterLandMedDokumenter(landMedDokumenterMedLandKode)
        }}
      >
        <option value="" disabled={true}>
          Velg land
        </option>
        {landListe.map((land) => (
          <option key={land.isoLandkode} value={land.isoLandkode}>
            {land.beskrivelse.tekst}
          </option>
        ))}
      </Select>
      <DokumenterForLand
        lesevisning={lesevisning}
        landMedDokumenter={landMedDokumenter}
        oppdaterLandMedDokumenter={oppdaterLandMedDokumenter}
      />
    </Flex>
  )
}
