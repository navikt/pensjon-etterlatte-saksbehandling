import { Link } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { useCallback } from 'react'

export enum PersonOversiktLenkeStorage {
  FnrPerson = 'fnrPerson',
}

const PersonOversiktLenke = ({ fnr }: { fnr: string | null }) => {
  const setFnrPersonStorage = useCallback(() => {
    if (fnr && fnrHarGyldigFormat(fnr)) {
      localStorage.setItem(PersonOversiktLenkeStorage.FnrPerson, fnr)
    }
  }, [fnr])

  return (
    <Link href="/person" onClick={setFnrPersonStorage}>
      {fnr ? fnr : ''}
    </Link>
  )
}

export default PersonOversiktLenke
