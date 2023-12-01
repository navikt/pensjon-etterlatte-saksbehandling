import { isFailure, Result } from '~shared/api/apiUtils'

export const IsFailure = ({ children, apiResult }: { apiResult: Result<any>; children: React.ReactNode }) => {
  if (isFailure(apiResult)) {
    if (apiResult.error.status === 502) {
      return null
    } else {
      return children
    }
  } else {
    return null
  }
}
