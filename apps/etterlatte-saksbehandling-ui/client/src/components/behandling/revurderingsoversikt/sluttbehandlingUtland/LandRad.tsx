import { HStack, Select } from '@navikt/ds-react'
import DokumenterForLand from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumenterForLand'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { ILand } from '~utils/kodeverk'

export default function LandRad({
  lesevisning = false,
  landMedDokumenter,
  oppdaterLandMedDokumenter,
  landListe,
  label = undefined,
}: {
  lesevisning: boolean
  landMedDokumenter: LandMedDokumenter
  oppdaterLandMedDokumenter: (oppdatertLandMedDokumenter: LandMedDokumenter) => void
  landListe: ILand[]
  label?: string
}) {
  return (
    <HStack gap="space-4" align="start">
      <Select
        readOnly={lesevisning}
        style={{ marginRight: '2rem', minWidth: '10rem', maxWidth: '12rem' }}
        label={lesevisning ? 'Valgt land' : 'Velg land'}
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
        label={label}
      />
    </HStack>
  )
}
