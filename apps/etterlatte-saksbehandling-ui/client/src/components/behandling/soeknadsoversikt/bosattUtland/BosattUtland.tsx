import { MottatteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/MottatteSeder'
import { SendteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/SendteSeder'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'

export const BosattUtland = () => {
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)

  useEffect(() => {
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  return (
    <>
      {isPending(hentAlleLandRequest) && <Spinner visible={true} label="Henter land" />}
      {alleLandKodeverk && (
        <>
          <MottatteSeder landliste={alleLandKodeverk as ILand[]} />
          <SendteSeder landliste={alleLandKodeverk as ILand[]} />
        </>
      )}
    </>
  )
}
