import { Button, Select } from '@navikt/ds-react'
import DokumentRad from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumentRad'
import { LandMedDokumenter } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLand'
import React, { useState } from 'react'
import { ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'

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
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>('')
  return (
    <Flex>
      <Select
        style={{ marginRight: '2rem', minWidth: '10rem', maxWidth: '12rem' }}
        label="Velg land"
        value={valgtLandIsoKode || ''}
        onChange={(e) => {
          setValgtLandIsoKode(e.target.value)
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
      <div>
        {landMedDokumenter.dokumenter.map((e, i) => {
          const oppdaterdokumenter = (field: string, value: string) => {
            const oppdaterteDokumenter = landMedDokumenter.dokumenter.map((doc, idx) =>
              idx === i ? { ...doc, [field]: value } : doc
            )
            const landMedDokumenterNy = { ...landMedDokumenter, dokumenter: oppdaterteDokumenter }
            oppdaterLandMedDokumenter(landMedDokumenterNy)
          }

          const fjernDokument = () => {
            const oppdaterteDokumenter = landMedDokumenter.dokumenter.filter((_, idx) => idx !== i)
            const landMedDokumenterNy = { ...landMedDokumenter, dokumenter: oppdaterteDokumenter }
            oppdaterLandMedDokumenter(landMedDokumenterNy)
          }
          return (
            <DokumentRad key={i} dokument={e} oppdaterDokument={oppdaterdokumenter} fjernDokument={fjernDokument} />
          )
        })}
        <div>
          <Button
            onClick={() => {
              oppdaterLandMedDokumenter({
                ...landMedDokumenter,
                dokumenter: landMedDokumenter.dokumenter.concat([{ dokumenttype: '', dato: undefined, kommentar: '' }]),
              })
            }}
            variant="tertiary"
          >
            Legg til dokument
          </Button>
        </div>
      </div>
    </Flex>
  )
}
