import DokumentRad from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/DokumentRad'
import { Button } from '@navikt/ds-react'
import React from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'

export default function DokumenterForLand({
  lesevisning,
  landMedDokumenter,
  oppdaterLandMedDokumenter,
  label = undefined,
}: {
  lesevisning: boolean
  landMedDokumenter: LandMedDokumenter
  oppdaterLandMedDokumenter: (oppdatertLandMedDokumenter: LandMedDokumenter) => void
  label?: string
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
            lesevisning={lesevisning}
            key={i}
            dokument={e}
            oppdaterDokument={oppdaterdokumenterForLand}
            fjernDokument={fjernDokumentForLand}
            label={label}
          />
        )
      })}
      <div>
        {!lesevisning && (
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
        )}
      </div>
    </div>
  )
}
