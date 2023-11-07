import { MottatteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/MottatteSeder'
import { SendteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/SendteSeder'

export const BosattUtland = () => {
  return (
    <>
      <MottatteSeder />
      <SendteSeder />
    </>
  )
}
