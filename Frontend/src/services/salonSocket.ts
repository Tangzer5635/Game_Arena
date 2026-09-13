import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { Salon } from "../types/salon";

const API_URL = "http://localhost:8080";

export const connectToSalon = (
    code: string,
    onUpdate: (salon: Salon) => void,
    onDeleted: () => void
): Client => {
    const client = new Client({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        reconnectDelay: 5000,

        onConnect: () => {
            client.subscribe(`/topic/salon/${code}`, (message: IMessage) => {
                const salon: Salon = JSON.parse(message.body);
                onUpdate(salon);
            });

            client.subscribe(`/topic/salon/${code}/deleted`, () => {
                onDeleted();
            });
        },

        onStompError: (frame) => {
            console.error("Erreur STOMP :", frame.headers["message"]);
        },

        onWebSocketError: (error) => {
            console.error("Erreur WebSocket :", error);
        },
    });

    client.activate();

    return client;
};
