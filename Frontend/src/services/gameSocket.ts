import {
    Client,
    type IMessage,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

const API_URL = "http://localhost:8080";

/*
 * ============================
 * TYPES
 * ============================
 */

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

    saisieLibre: boolean;
}

export interface GameScores {
    scores: Record<string, number>;

    answeredCount: number;
    totalPlayers: number;
}

export interface GameAnswerResult {
    userId: number;

    correct: boolean;

    correctReponseId: number | null;

    pointsEarned: number;

    score: number;

    elapsedMs: number;

    streak: number;

    multiplier: number;
}

export interface GameReveal {
    correctReponseId: number | null;

    correctAnswer: string | null;

    scores: Record<string, number>;
}

export interface GameCountdown {
    value: number;
}

export interface GameCallbacks {

    onQuestion: (
        question: GameQuestion
    ) => void;

    onScores: (
        scores: GameScores
    ) => void;

    onAnswerResult: (
        result: GameAnswerResult
    ) => void;

    onReveal: (
        reveal: GameReveal
    ) => void;

    onResults: (
        results: GameScores
    ) => void;

    onCountdown: (
        countdown: GameCountdown
    ) => void;

    onConnected: () => void;
}

/*
 * ============================
 * RÉCUPÉRATION DU JWT
 * ============================
 */

function getAuthHeaders(): Record<string, string> {

    const token =
        localStorage.getItem("token");

    if (!token) {

        console.error(
            "Aucun JWT trouvé dans localStorage."
        );

        return {};
    }

    console.log(
        "JWT trouvé pour WebSocket."
    );

    return {
        Authorization: `Bearer ${token}`,
    };
}

/*
 * ============================
 * CONNEXION
 * ============================
 */

export const connectToGame = (
    code: string,
    userId: number,
    callbacks: GameCallbacks
): Client => {

    const token =
        localStorage.getItem("token");

    console.log(
        "JWT présent pour WebSocket :",
        !!token
    );

    const client = new Client({

        /*
         * SockJS
         */
        webSocketFactory: () => {

            return new SockJS(
                `${API_URL}/ws`
            );
        },

        reconnectDelay: 5000,

        /*
         * JWT lors du CONNECT STOMP.
         */
        connectHeaders:
            getAuthHeaders(),

        debug: (message) => {

            console.log(
                "[STOMP]",
                message
            );
        },

        /*
         * ============================
         * CONNECTÉ
         * ============================
         */

        onConnect: () => {

            console.log(
                "WebSocket/STOMP connecté."
            );

            /*
             * Questions
             */
            client.subscribe(
                `/topic/game/${code}/question`,
                (message: IMessage) => {

                    const question:
                        GameQuestion =
                        JSON.parse(
                            message.body
                        );

                    console.log(
                        "Question reçue :",
                        question
                    );

                    callbacks.onQuestion(
                        question
                    );
                }
            );

            /*
             * Scores
             */
            client.subscribe(
                `/topic/game/${code}/scores`,
                (message: IMessage) => {

                    const scores:
                        GameScores =
                        JSON.parse(
                            message.body
                        );

                    callbacks.onScores(
                        scores
                    );
                }
            );

            /*
             * Résultat personnel
             */
            client.subscribe(
                `/topic/game/${code}/answer-result/${userId}`,
                (message: IMessage) => {

                    const result:
                        GameAnswerResult =
                        JSON.parse(
                            message.body
                        );

                    callbacks.onAnswerResult(
                        result
                    );
                }
            );

            /*
             * Révélation
             */
            client.subscribe(
                `/topic/game/${code}/reveal`,
                (message: IMessage) => {

                    const reveal:
                        GameReveal =
                        JSON.parse(
                            message.body
                        );

                    callbacks.onReveal(
                        reveal
                    );
                }
            );

            /*
             * Résultats
             */
            client.subscribe(
                `/topic/game/${code}/results`,
                (message: IMessage) => {

                    const results:
                        GameScores =
                        JSON.parse(
                            message.body
                        );

                    callbacks.onResults(
                        results
                    );
                }
            );

            /*
             * Compte à rebours
             */
            client.subscribe(
                `/topic/game/${code}/countdown`,
                (message: IMessage) => {

                    const countdown:
                        GameCountdown =
                        JSON.parse(
                            message.body
                        );

                    callbacks.onCountdown(
                        countdown
                    );
                }
            );

            callbacks.onConnected();
        },

        onStompError: (frame) => {

            console.error(
                "Erreur STOMP :",
                frame.headers["message"]
            );

            console.error(
                "Détails :",
                frame.body
            );
        },

        onWebSocketError: (error) => {

            console.error(
                "Erreur WebSocket :",
                error
            );
        },

        onDisconnect: () => {

            console.log(
                "WebSocket déconnecté."
            );
        },
    });

    client.activate();

    return client;
};

/*
 * ============================
 * LANCER LA PARTIE
 * ============================
 */

export const sendStartGame = (
    client: Client,
    code: string
) => {

    console.log(
        "Envoi lancement partie :",
        `/app/game/${code}/start`
    );

    client.publish({

        destination:
            `/app/game/${code}/start`,

        headers:
            getAuthHeaders(),

        body: "",
    });
};

/*
 * ============================
 * ENVOYER UNE RÉPONSE
 * ============================
 */

export const sendAnswer = (
    client: Client,
    code: string,
    reponseId: number | null,
    answer: string
) => {

    console.log(
        "Envoi réponse..."
    );

    client.publish({

        destination:
            `/app/game/${code}/answer`,

        headers:
            getAuthHeaders(),

        body: JSON.stringify({

            reponseId,

            answer,
        }),
    });
};

