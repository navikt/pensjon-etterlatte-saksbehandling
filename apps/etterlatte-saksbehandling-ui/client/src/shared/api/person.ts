const path = process.env.REACT_APP_VEDTAK_URL

export const getPerson = async (fnr: string) => {
  try {
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    if (result.ok) {
      return { status: result.status, data: await result.json() }
    }
  } catch (e) {
    console.log('response errorrrrrr', e)
    throw new Error('Det skjedde en feil')
  }
}
