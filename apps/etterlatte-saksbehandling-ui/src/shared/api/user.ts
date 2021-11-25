
interface IApiResponse<T> {
  status: number;
  data?: T
}

export const login = async (): Promise<IApiResponse<any>> => {
  // Bare tester litt
  try{
    const result: Response = await fetch("https://etterlatte-overvaaking.dev.intern.nav.no/")
    return {
      status: result.status,
      data: await result.json()
    }
  } catch(e) {
    console.log(e);
    return {status: 500};
  }
}