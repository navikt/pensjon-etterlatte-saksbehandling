export interface IApiResponse<T> {
  status: number;
  data?: T
}