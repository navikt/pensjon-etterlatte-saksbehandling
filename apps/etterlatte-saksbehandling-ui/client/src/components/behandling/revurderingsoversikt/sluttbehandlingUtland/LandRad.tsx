import { Select } from '@navikt/ds-react'
import { LandMedDokumenter } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import DokumenterForLand from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumenterForLand'

const Flex = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;

  div:first-child {
    margin-bottom: auto;
  }
`

export default function LandRad({
  landMedDokumenter,
  oppdaterLandMedDokumenter,
  landListe,
}: {
  landMedDokumenter: LandMedDokumenter
  oppdaterLandMedDokumenter: (oppdatertLandMedDokumenter: LandMedDokumenter) => void
  landListe: ILand[]
}) {
  return (
    <Flex>
      <Select
        style={{ marginRight: '2rem', minWidth: '10rem', maxWidth: '12rem' }}
        label="Velg land"
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
      <DokumenterForLand landMedDokumenter={landMedDokumenter} oppdaterLandMedDokumenter={oppdaterLandMedDokumenter} />
    </Flex>
  )
}
