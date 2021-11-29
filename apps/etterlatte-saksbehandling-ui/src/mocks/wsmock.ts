class WebSocket {
  addEventListener() {}
}
export const ws = () => {
    (window as any).WebSocket = WebSocket;
};
