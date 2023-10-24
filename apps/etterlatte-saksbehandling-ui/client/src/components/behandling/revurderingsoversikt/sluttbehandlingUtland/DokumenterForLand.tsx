import { LandMedDokumenter } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLand'
import DokumentRad from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumentRad'
import { Button } from '@navikt/ds-react'
import React from 'react'

export default function DokumenterForLand({
  landMedDokumenter,
  oppdaterLandMedDokumenter,
}: {
  landMedDokumenter: LandMedDokumenter
  oppdaterLandMedDokumenter: (oppdatertLandMedDokumenter: LandMedDokumenter) => void
}) {
  return (
    <div>
      {landMedDokumenter.dokumenter.map((e, i) => {
        const oppdaterdokumenterForLand = (field: string, value: string) => {
          const oppdaterteDokumenter = landMedDokumenter.dokumenter.map((doc, idx) =>
            idx === i ? { ...doc, [field]: value } : doc
          )
          const landMedDokumenterNy = { ...landMedDokumenter, dokumenter: oppdaterteDokumenter }
          oppdaterLandMedDokumenter(landMedDokumenterNy)
        }

        const fjernDokumentForLand = () => {
          const oppdaterteDokumenter = landMedDokumenter.dokumenter.filter((_, idx) => idx !== i)
          const landMedDokumenterNy = { ...landMedDokumenter, dokumenter: oppdaterteDokumenter }
          oppdaterLandMedDokumenter(landMedDokumenterNy)
        }
        return (
          <DokumentRad
            key={i}
            dokument={e}
            oppdaterDokument={oppdaterdokumenterForLand}
            fjernDokument={fjernDokumentForLand}
          />
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
  )
}
