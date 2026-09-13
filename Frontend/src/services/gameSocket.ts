import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const API_URL = "http://localhost:8080";

// ── Types ──

export interface GameReponse {
    id: number;
    text: string;
}

export interface GameQuestion {
    questionId: number;
    text: string;
    reponses: GameReponse[];
    questionNumber: number;
    totalQuestions: number;
}

export interface GameScores {
    scores: Record<string, number>;
    answeredCount: number;
    totalPlayers: number;
}

export interface GameAnswerResult {
    userId: number;
    correct: boolean;
    correctReponseId: number;
    score: number;
}

// ── Callbacks ──

export interface GameCallbacks {
    onQuestion: (question: GameQuestion) => void;
    onScores: (scores: GameScores) => void;
    onAnswerResult: (result: GameAnswerResult) => void;
    onResults: (results: GameScores) => void;
    onConnected: () => void;
}

// ── Client ──

export const connectToGame = (
    code: string,
    userId: number,
    callbacks: GameCallbacks
): Client => {
    const client = new Client({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        reconnectDelay: 5000,

        debug: (message) => {
            console.log("[STOMP]", message);
        },

        onConnect: () => {
            console.log("Game WebSocket connecté");

            client.subscribe(`/topic/game/${code}/question`, (msg: IMessage) => {
                callbacks.onQuestion(JSON.parse(msg.body));
            });

            client.subscribe(`/topic/game/${code}/scores`, (msg: IMessage) => {
                callbacks.onScores(JSON.parse(msg.body));
            });

            client.subscribe(`/topic/game/${code}/answer-result/${userId}`, (msg: IMessage) => {
                callbacks.onAnswerResult(JSON.parse(msg.body));
            });

            client.subscribe(`/topic/game/${code}/results`, (msg: IMessage) => {
                callbacks.onResults(JSON.parse(msg.body));
            });

            callbacks.onConnected();
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

export const sendStartGame = (client: Client, code: string) => {
    client.publish({
        destination: `/app/game/${code}/start`,
        body: "",
    });
};

export const sendAnswer = (
    client: Client,
    code: string,
    userId: number,
    reponseId: number
) => {
    client.publish({
        destination: `/app/game/${code}/answer`,
        body: JSON.stringify({ userId, reponseId }),
    });
};
